package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import kotlin.time.Clock

class ResumeTimerUseCase {
    operator fun invoke(currentState: TimerState): Result<TimerState> {
        if (currentState.status != TimerStatus.PAUSED) {
            return Result.failure(IllegalStateException("Timer is not paused"))
        }

        return try {
            val now = Clock.System.now()

            // Recalculate start time to maintain remaining duration
            val resumedState = currentState.copy(
                status = TimerStatus.RUNNING,
                startedAt = now,
                pausedAt = null,
                totalDuration = currentState.remainingDuration,  // New total is the remaining
            )

            Result.success(resumedState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}