package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Restores timer state from a draft session.
 * Recalculates remaining time based on when the timer was last saved.
 */
class RestoreTimerStateUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<TimerState?> {
        return try {
            val session = teaSessionRepository.getById(sessionId)
                ?: return Result.failure(IllegalStateException("Session not found: $sessionId"))

            // No timer state saved
            if (session.timerStatus == null) {
                return Result.success(null)
            }

            val now = Clock.System.now()
            val savedRemainingMs = session.timerRemainingMs ?: 0L

            val (newStatus, newRemainingMs) = when (session.timerStatus) {
                TimerStatus.RUNNING -> {
                    // Timer was running - recalculate based on elapsed time since started
                    val startedAt = session.timerStartedAt
                    if (startedAt != null) {
                        val elapsedSinceStart = now - startedAt
                        val totalDurationMs = session.brewingTime.inWholeMilliseconds
                        val newRemaining = totalDurationMs - elapsedSinceStart.inWholeMilliseconds

                        if (newRemaining <= 0) {
                            // Timer has completed while app was closed
                            TimerStatus.COMPLETE to 0L
                        } else {
                            TimerStatus.RUNNING to newRemaining
                        }
                    } else {
                        // No start time - use saved remaining as fallback
                        TimerStatus.RUNNING to savedRemainingMs
                    }
                }

                TimerStatus.PAUSED -> {
                    // Timer was paused - use saved remaining time directly
                    TimerStatus.PAUSED to savedRemainingMs
                }

                TimerStatus.COMPLETE -> {
                    // Timer was already complete
                    TimerStatus.COMPLETE to 0L
                }

                TimerStatus.NOT_STARTED -> {
                    // Should not happen, but handle it
                    return Result.success(null)
                }
            }

            val timerState = TimerState(
                sessionId = session.id,
                teaId = session.teaId,
                teaName = "", // Will be populated by caller
                steepNumber = session.steepNumber,
                totalDuration = session.brewingTime,
                remainingDuration = newRemainingMs.milliseconds,
                status = newStatus,
                startedAt = session.timerStartedAt,
                pausedAt = session.timerPausedAt,
            )

            Result.success(timerState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
