package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Use case to adjust the timer's duration. A timer not yet started stops at one second, since
 * Start needs a duration; one already counting stops at zero, which completes it.
 */
class AdjustTimeUseCase {
    operator fun invoke(
        currentState: TimerState,
        adjustment: Duration,
    ): Result<TimerState> {
        val remaining = when (currentState.status) {
            TimerStatus.NOT_STARTED -> {
                val adjusted = (currentState.totalDuration + adjustment).coerceAtLeast(1.seconds)
                return Result.success(
                    currentState.copy(totalDuration = adjusted, remainingDuration = adjusted),
                )
            }
            // The clock stopped at the pause, so time spent paused isn't counted
            TimerStatus.PAUSED -> currentState.remainingDuration
            TimerStatus.RUNNING -> currentState.resolvedAt(Clock.System.now()).remainingDuration
            TimerStatus.COMPLETE -> return Result.failure(IllegalStateException("Timer is complete"))
        }

        val newRemaining = (remaining + adjustment).coerceAtLeast(Duration.ZERO)
        return Result.success(
            currentState.copy(
                totalDuration = currentState.totalDuration - remaining + newRemaining,
                remainingDuration = newRemaining,
            ),
        )
    }
}
