package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.Uuid

/**
 * Adds the next steep, which becomes the session's [SessionWithSteeps.currentSteep].
 *
 * A finished session stays finished: the steep was forgotten at the time, so it's stamped as
 * following the last one rather than now.
 */
class AddSteepUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val recomputeStats: RecomputeStatsUseCase,
) {
    suspend operator fun invoke(
        sessionId: String,
        brewingTime: Duration,
        temperatureCelsius: Double,
        waterQuantityMl: Double,
    ): Result<SessionWithSteeps> = runCatching {
        transactor {
            val current = teaSessionRepository.getSession(sessionId)
                ?: error("Session not found: $sessionId")
            val last = current.currentSteep
            val now = Clock.System.now()
            val steep = Steep(
                id = Uuid.random().toString(),
                number = last.number + 1,
                timestamp = when (current.session.status) {
                    SessionStatus.COMPLETED -> last.timestamp + last.brewingTime
                    SessionStatus.IN_PROGRESS -> now
                },
                brewingTime = brewingTime,
                temperatureCelsius = temperatureCelsius,
                waterQuantityMl = waterQuantityMl,
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
