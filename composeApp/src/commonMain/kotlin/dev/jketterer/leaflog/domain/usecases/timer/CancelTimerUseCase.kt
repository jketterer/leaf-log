package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import kotlin.time.Duration

/**
 * Use case to cancel a timer.
 * Session remains as draft for user to complete later.
 */
class CancelTimerUseCase {
    operator fun invoke(currentState: TimerState): Result<TimerState> {
        return try {
            val cancelledState = currentState.copy(
                status = TimerStatus.NOT_STARTED,
                remainingDuration = Duration.ZERO,
                startedAt = null,
                pausedAt = null,
            )

            Result.success(cancelledState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}