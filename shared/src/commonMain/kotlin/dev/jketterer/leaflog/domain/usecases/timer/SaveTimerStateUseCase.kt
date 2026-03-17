package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock

/**
 * Saves timer state to a session for later restoration.
 * Called when timer is paused, completed, or app backgrounds.
 */
class SaveTimerStateUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(timerState: TimerState): Result<Unit> {
        val sessionId = timerState.sessionId ?: return Result.failure(
            IllegalArgumentException("Cannot save timer state without session ID")
        )

        return try {
            val session = teaSessionRepository.getById(sessionId)
                ?: return Result.failure(IllegalStateException("Session not found: $sessionId"))

            val now = Clock.System.now()
            val updatedSession = session.copy(
                brewingTime = timerState.totalDuration,
                timerStatus = timerState.status,
                timerStartedAt = timerState.startedAt,
                timerPausedAt = when (timerState.status) {
                    TimerStatus.PAUSED -> now
                    else -> timerState.pausedAt
                },
                timerRemainingMs = timerState.remainingDuration.inWholeMilliseconds,
                updatedAt = now,
            )

            teaSessionRepository.upsert(updatedSession)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears timer state from a session (e.g., when session is completed or timer is cancelled).
     */
    suspend fun clear(sessionId: String): Result<Unit> {
        return try {
            val session = teaSessionRepository.getById(sessionId)
                ?: return Result.failure(IllegalStateException("Session not found: $sessionId"))

            val updatedSession = session.copy(
                timerStatus = null,
                timerStartedAt = null,
                timerPausedAt = null,
                timerRemainingMs = null,
                updatedAt = Clock.System.now(),
            )

            teaSessionRepository.upsert(updatedSession)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
