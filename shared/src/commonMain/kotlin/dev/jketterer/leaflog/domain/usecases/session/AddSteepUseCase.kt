package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.carriedForward
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.Uuid

/**
 * Adds the next steep, which becomes the session's [SessionWithSteeps.currentSteep]. It starts with
 * the last steep's tags, unconfirmed.
 *
 * A finished session stays finished: the steep was forgotten at the time, so it's stamped as
 * following the last one rather than now.
 */
class AddSteepUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val recomputeStats: RecomputeStatsUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        sessionId: String,
        brewingTime: Duration,
        temperatureCelsius: Double,
        waterQuantityMl: Double,
    ): Result<SessionWithSteeps> = runCatching {
        transactor {
            val current = teaSessionRepository.getSession(sessionId)
                ?: throw DomainFailure.NotFound("session $sessionId")
            val last = current.currentSteep
            val now = clock.now()
            val steep = Steep(
                id = Uuid.random().toString(),
                number = last.number + 1,
                timestamp = when (current.session.status) {
                    SessionStatus.COMPLETED -> last.timestamp + last.brewedTime
                    SessionStatus.IN_PROGRESS -> now
                },
                brewingTime = brewingTime,
                temperatureCelsius = temperatureCelsius,
                waterQuantityMl = waterQuantityMl,
                tags = last.tags.carriedForward(),
                createdAt = now,
                updatedAt = now,
            )
            steep.requireValid()

            val updated = current.copy(steeps = current.steeps + steep)
            teaSessionRepository.saveSession(updated)
            recomputeStats(current.session.teaId)
            updated
        }
    }
}
