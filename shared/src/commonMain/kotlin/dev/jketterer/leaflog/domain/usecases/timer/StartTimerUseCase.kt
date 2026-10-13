package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Use case to start a brewing timer on the session's current steep.
 */
class StartTimerUseCase {
    operator fun invoke(
        session: SessionWithSteeps,
        duration: Duration,
    ): Result<TimerState> {
        if (duration <= Duration.ZERO) {
            return Result.failure(IllegalArgumentException("Duration must be greater than 0"))
        }

        val steep = session.currentSteep
        return Result.success(
            TimerState(
                sessionId = session.session.id,
                teaId = session.session.teaId,
                teaName = "",  // Will be populated by ViewModel from tea data
                steepNumber = steep.number,
                totalDuration = duration,
                remainingDuration = duration,
                status = TimerStatus.RUNNING,
                startedAt = Clock.System.now(),
                pausedAt = null,
                brewingTemperatureCelsius = steep.temperatureCelsius,
            ),
        )
    }
}
