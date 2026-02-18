package dev.jketterer.leaflog.presentation.ui.screens.timer

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.UserPreferences
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
    val vessel: BrewingVessel? = null,
    val rating: Float = 0f,
    val notes: String? = null,
    val photos: List<String> = emptyList(),
    val showStopConfirmation: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val showNextSteepDialog: Boolean = false,
    val nextSteepDuration: Duration? = null,
    val nextSteepTemperature: Int? = null,
    val nextSteepWaterQuantity: Int? = null,
    val showSaveConfigurationDialog: Boolean = false,
    val savedSession: TeaSession? = null, // Session that was just saved
    val showDiscardConfirmation: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val userPreferences: UserPreferences = UserPreferences(),
) {
    /**
     * Formatted remaining time as MM:SS.
     * Uses ceiling division to round up fractional seconds.
     */
    val formattedTime: String
        get() {
            val remaining = when (timerState.status) {
                TimerStatus.RUNNING, TimerStatus.PAUSED -> timerState.remainingDuration
                else -> timerState.totalDuration
            }

            // Round up to nearest second for display (ceiling division)
            val milliseconds = remaining.inWholeMilliseconds
            val totalSeconds = if (milliseconds > 0) {
                (milliseconds + 999) / 1000  // Ceiling: round up if any fractional ms
            } else {
                0L
            }

            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "${minutes}:${seconds.toString().padStart(2, '0')}"
        }

    /**
     * Whether the completion screen should be shown.
     */
    val showCompletionScreen: Boolean
        get() = timerState.isComplete
}