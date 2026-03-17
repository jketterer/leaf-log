package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import kotlin.time.Clock
import kotlin.time.Duration

class PauseTimerUseCase {
    operator fun invoke(currentState: TimerState): Result<TimerState> {
        if (currentState.status != TimerStatus.RUNNING) {
            return Result.failure(IllegalStateException("Timer is not running"))
        }

        if (currentState.startedAt == null) {
            return Result.failure(IllegalStateException("Timer has no start time"))
        }

        return try {
            val now = Clock.System.now()
            val elapsed = now - currentState.startedAt
            val remaining = (currentState.totalDuration - elapsed).coerceAtLeast(Duration.ZERO)

            val pausedState = currentState.copy(
                status = TimerStatus.PAUSED,
                remainingDuration = remaining,
                pausedAt = now,
            )

            Result.success(pausedState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}