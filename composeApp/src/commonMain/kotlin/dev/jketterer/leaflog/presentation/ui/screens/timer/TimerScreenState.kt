package dev.jketterer.leaflog.presentation.ui.screens.timer

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * UI State for the Timer screen.
 *
 * All properties have default values for easier testing/debugging.
 */
data class TimerScreenState(
    val timerState: TimerState = TimerState(),
    val session: TeaSession? = null,
    val tea: Tea? = null,
    val rating: Float = 0f,
    val notes: String? = null,
    val photos: List<String> = emptyList(),
    val showStopConfirmation: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val showNextSteepDialog: Boolean = false,
    val nextSteepDuration: Duration? = null,
    val nextSteepTemperature: Int? = null,
    val nextSteepWaterQuantity: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /**
     * Formatted remaining time as MM:SS.
     */
    val formattedTime: String
        get() {
            val remaining = if (timerState.isRunning) {
                timerState.remainingDuration.plus(1.seconds)
            } else {
                timerState.totalDuration
            }
            val minutes = remaining.inWholeMinutes
            val seconds = remaining.inWholeSeconds % 60
            return "${minutes}:${seconds.toString().padStart(2, '0')}"
        }

    /**
     * Whether the completion screen should be shown.
     */
    val showCompletionScreen: Boolean
        get() = timerState.isComplete

    /**
     * Whether controls are enabled.
     */
    val controlsEnabled: Boolean
        get() = !isLoading && timerState.status != TimerStatus.NOT_STARTED
}