package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Restores the timer saved on the session's current steep, or null if none was saved.
 * Recalculates remaining time based on when the timer was last saved.
 */
class RestoreTimerStateUseCase(
    private val clock: Clock,
) {
    operator fun invoke(session: SessionWithSteeps): TimerState? {
        val steep = session.currentSteep
        val now = clock.now()
        val savedRemainingMs = steep.timerRemainingMs ?: 0L

        val (newStatus, newRemainingMs) = when (steep.timerStatus) {
            // No timer state saved
            null, TimerStatus.NOT_STARTED -> return null

            TimerStatus.RUNNING -> {
                // Timer was running - recalculate based on elapsed time since started
                val startedAt = steep.timerStartedAt
                if (startedAt != null) {
                    val elapsedSinceStart = now - startedAt
                    val totalDurationMs = steep.timerTotalMs ?: steep.brewingTime.inWholeMilliseconds
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

            // Timer was paused - use saved remaining time directly
            TimerStatus.PAUSED -> TimerStatus.PAUSED to savedRemainingMs

            TimerStatus.COMPLETE -> TimerStatus.COMPLETE to 0L
        }

        return TimerState(
            sessionId = session.session.id,
            teaId = session.session.teaId,
            teaName = "", // Will be populated by caller
            steepNumber = steep.number,
            totalDuration = steep.timerTotalMs?.milliseconds ?: steep.brewingTime,
            remainingDuration = newRemainingMs.milliseconds,
            status = newStatus,
            startedAt = steep.timerStartedAt,
            pausedAt = steep.timerPausedAt,
            brewingTemperatureCelsius = steep.temperatureCelsius,
        )
    }
}
