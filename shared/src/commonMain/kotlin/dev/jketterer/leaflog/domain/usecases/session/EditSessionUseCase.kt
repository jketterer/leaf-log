package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock

/**
 * Applies [edit] to the session as stored, so callers name only what they change and `null` clears.
 * Only the set-up can change: vessel, water type, leaf, location and rinse.
 */
class EditSessionUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val recomputeStats: RecomputeStatsUseCase,
) {
    suspend operator fun invoke(
        sessionId: String,
        edit: (Session) -> Session,
    ): Result<SessionWithSteeps> = runCatching {
        transactor {
            val current = teaSessionRepository.getSession(sessionId)
                ?: throw DomainFailure.NotFound("session $sessionId")
            val session = current.session
            val edited = edit(session).let { it.copy(location = it.location.blankToNull()) }

            val untouched = edited.copy(
                vesselId = session.vesselId,
                waterType = session.waterType,
                teaQuantityGrams = session.teaQuantityGrams,
                location = session.location,
                rinsed = session.rinsed,
            )
            require(untouched == session) {
                "Only a session's vessel, water type, leaf, location and rinse can be edited"
            }
            edited.requireValid()

            val updated = current.withSession(edited, Clock.System.now())
            teaSessionRepository.saveSession(updated)
            recomputeStats(session.teaId)
            updated
        }
    }
}
