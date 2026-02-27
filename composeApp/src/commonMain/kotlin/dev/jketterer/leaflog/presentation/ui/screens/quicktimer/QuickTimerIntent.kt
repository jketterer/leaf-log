package dev.jketterer.leaflog.presentation.ui.screens.quicktimer

import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

/**
 * All possible user actions on the Quick Timer screen.
 */
sealed interface QuickTimerIntent {
    // Timer control
    data class Initialize(val durationSeconds: Int) : QuickTimerIntent
    data object StartTimer : QuickTimerIntent
    data object PauseTimer : QuickTimerIntent
    data object ResumeTimer : QuickTimerIntent
    data class AdjustTime(val adjustment: Duration) : QuickTimerIntent
    data object ResetTimer : QuickTimerIntent
    data object ConfirmReset : QuickTimerIntent
    data object CancelReset : QuickTimerIntent
    data object StopTimer : QuickTimerIntent
    data object ConfirmStop : QuickTimerIntent
    data object CancelStop : QuickTimerIntent
    data object BackClicked : QuickTimerIntent

    // Details sheet
    data object ShowDetailsSheet : QuickTimerIntent
    data object HideDetailsSheet : QuickTimerIntent
    data object NextDetailsStep : QuickTimerIntent
    data object PreviousDetailsStep : QuickTimerIntent

    // Tea selection (Step 1)
    data class TeaSearchQueryChanged(val query: String) : QuickTimerIntent
    data class TeaSelected(val teaId: String) : QuickTimerIntent

    // Vessel selection (Step 1)
    data class VesselSelected(val vesselId: String) : QuickTimerIntent

    // Brewing parameters (Step 2)
    data class TeaQuantityChanged(val quantity: String) : QuickTimerIntent
    data class TeaBagModeChanged(val isTeaBag: Boolean) : QuickTimerIntent
    data class TemperatureChanged(val temperature: String) : QuickTimerIntent
    data object ToggleTemperatureUnit : QuickTimerIntent
    data class WaterQuantityChanged(val quantity: String) : QuickTimerIntent
    data object ToggleVolumeUnit : QuickTimerIntent
    data class WaterTypeSelected(val waterType: WaterType) : QuickTimerIntent

    // Rating and notes
    data class RatingChanged(val rating: Float?) : QuickTimerIntent
    data class NotesChanged(val notes: String) : QuickTimerIntent

    // Completion
    data object ShowCompletionDialog : QuickTimerIntent
    data object DismissCompletionDialog : QuickTimerIntent
    data object SaveSession : QuickTimerIntent
    data object ContinueToNextSteep : QuickTimerIntent
    data object DiscardSession : QuickTimerIntent

    // Configuration saving
    data class SaveConfigurationClicked(val customLabel: String?) : QuickTimerIntent
    data object SkipSaveConfiguration : QuickTimerIntent
}
