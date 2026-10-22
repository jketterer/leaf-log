package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Applies [edit] to the session as stored, so callers name only what they change and `null` clears.
 * Only the tea, the set-up and the note can change: tea, vessel, water type, leaf, location, rinse
 * and notes. A session moved to another tea no longer used that tea's method or plan.
 *
 * [startedAt] moves the session: every steep shifts by the same amount, so the gaps between them
 * stay as brewed.
 */
class EditSessionUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val recomputeStats: RecomputeStatsUseCase,
) {
    suspend operator fun invoke(
        sessionId: String,
        startedAt: Instant? = null,
        edit: (Session) -> Session,
    ): Result<SessionWithSteeps> = runCatching {
        transactor {
            val current = teaSessionRepository.getSession(sessionId)
                ?: throw DomainFailure.NotFound("session $sessionId")
            val session = current.session
            val edited = edit(session).let {
                it.copy(location = it.location.blankToNull(), notes = it.notes.blankToNull())
            }

            val untouched = edited.copy(
                teaId = session.teaId,
                vesselId = session.vesselId,
                waterType = session.waterType,
                teaQuantityGrams = session.teaQuantityGrams,
                location = session.location,
                rinsed = session.rinsed,
                notes = session.notes,
            )
            require(untouched == session) {
                "Only a session's tea, vessel, water type, leaf, location, rinse and notes can be edited"
            }
            edited.requireValid()

            val teaChanged = edited.teaId != session.teaId
            val moved = if (teaChanged) {
                edited.copy(usedConfigurationId = null, planSessionId = null)
            } else {
                edited
            }

            val now = Clock.System.now()
            val shift = startedAt?.let { it - current.startedAt }
            val updated = current.withSession(moved, now).let { aggregate ->
                if (shift == null || shift == Duration.ZERO) aggregate
                else aggregate.copy(
                    steeps = aggregate.steeps.map {
                        it.copy(timestamp = it.timestamp + shift, updatedAt = now)
                    },
                )
            }
            teaSessionRepository.saveSession(updated)
            recomputeStats(session.teaId)
            if (teaChanged) recomputeStats(moved.teaId)
            updated
        }
    }
}
