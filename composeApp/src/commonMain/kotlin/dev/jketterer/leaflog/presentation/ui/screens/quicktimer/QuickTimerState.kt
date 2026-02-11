package dev.jketterer.leaflog.presentation.ui.screens.quicktimer

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * UI State for the Quick Timer screen.
 */
data class QuickTimerState(
    // Timer state
    val totalDuration: Duration = Duration.ZERO,
    val remainingDuration: Duration = Duration.ZERO,
    val status: TimerStatus = TimerStatus.NOT_STARTED,

    // Progressive details
    val selectedTea: Tea? = null,
    val selectedVessel: BrewingVessel? = null,
    val teaQuantityGrams: String = "",
    val temperatureCelsius: String = "",
    val waterQuantityMl: String = "",
    val waterType: WaterType = WaterType.FILTERED,
    val prefillSource: PrefillSource = PrefillSource.None,

    // Session notes and rating
    val rating: Float? = null,
    val notes: String = "",

    // Available options for selection
    val availableTeas: List<Tea> = emptyList(),
    val availableVessels: List<BrewingVessel> = emptyList(),
    val teaSearchQuery: String = "",

    // Sheet state
    val showDetailsSheet: Boolean = false,
    val detailsSheetStep: Int = 1, // 1 = Tea + Vessel, 2 = Params

    // Confirmation dialogs
    val showStopConfirmation: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val showSaveConfigurationDialog: Boolean = false,

    // Saved session (for configuration saving)
    val savedSession: TeaSession? = null,

    // Loading/error
    val isLoading: Boolean = false,
    val error: String? = null,

    // User preferences
    val userPreferences: UserPreferences = UserPreferences(),
) {
    /**
     * Progress from 0.0 (start) to 1.0 (complete).
     */
    val progress: Float
        get() = if (totalDuration > Duration.ZERO) {
            val elapsed = totalDuration - remainingDuration
            (elapsed.inWholeMilliseconds / totalDuration.inWholeMilliseconds.toFloat()).coerceIn(
                0f,
                1f,
            )
        } else {
            0f
        }

    /**
     * Formatted remaining time as MM:SS.
     * Uses ceiling division to round up fractional seconds.
     */
    val formattedTime: String
        get() {
            val remaining = when (status) {
                TimerStatus.RUNNING, TimerStatus.PAUSED -> remainingDuration
                else -> totalDuration
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

    val isRunning: Boolean
        get() = status == TimerStatus.RUNNING

    val isPaused: Boolean
        get() = status == TimerStatus.PAUSED

    val isComplete: Boolean
        get() = status == TimerStatus.COMPLETE

    val controlsEnabled: Boolean
        get() = !isLoading && status != TimerStatus.NOT_STARTED

    /**
     * Whether the user has filled in required details (tea + vessel + temp + water qty).
     */
    val hasRequiredDetails: Boolean
        get() = selectedTea != null &&
                selectedVessel != null &&
                temperatureCelsius.isNotBlank() &&
                waterQuantityMl.isNotBlank()

    /**
     * Filtered teas based on search query.
     */
    val filteredTeas: List<Tea>
        get() = if (teaSearchQuery.isBlank()) {
            availableTeas
        } else {
            availableTeas.filter {
                it.name.contains(teaSearchQuery, ignoreCase = true)
            }
        }
}
