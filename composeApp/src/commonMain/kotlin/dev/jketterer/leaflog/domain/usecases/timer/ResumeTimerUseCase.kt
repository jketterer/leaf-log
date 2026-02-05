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

            // Adjust startedAt to account for time already elapsed before pause,
            // preserving totalDuration so progress ring stays correct
            val alreadyElapsed = currentState.totalDuration - currentState.remainingDuration
            val resumedState = currentState.copy(
                status = TimerStatus.RUNNING,
                startedAt = now - alreadyElapsed,
                pausedAt = null,
            )

            Result.success(resumedState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}