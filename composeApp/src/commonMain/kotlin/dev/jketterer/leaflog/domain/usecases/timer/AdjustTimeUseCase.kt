package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Use case to adjust timer duration while running.
 * Prevents negative time and recalculates remaining duration.
 */
class AdjustTimeUseCase {
    operator fun invoke(
        currentState: TimerState,
        adjustment: Duration,
    ): Result<TimerState> {
        if (currentState.status != TimerStatus.RUNNING) {
            return Result.failure(IllegalStateException("Can only adjust running timer"))
        }

        if (currentState.startedAt == null) {
            return Result.failure(IllegalStateException("Timer has no start time"))
        }

        return try {
            val now = Clock.System.now()
            val elapsed = now - currentState.startedAt
            val currentRemaining =
                (currentState.totalDuration - elapsed).coerceAtLeast(Duration.ZERO)

            // Apply adjustment
            val newRemaining = (currentRemaining + adjustment).coerceAtLeast(Duration.ZERO)
            val newTotal = elapsed + newRemaining

            val adjustedState = currentState.copy(
                totalDuration = newTotal,
                remainingDuration = newRemaining,
            )

            Result.success(adjustedState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}