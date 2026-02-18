package dev.jketterer.leaflog.presentation.ui.screens.timer

import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

/**
 * All possible user actions on the Timer screen.
 */
sealed interface TimerIntent {
    data class Initialize(val sessionId: String) : TimerIntent
    data object StartTimer : TimerIntent
    data object PauseTimer : TimerIntent
    data object ResumeTimer : TimerIntent
    data class AdjustTime(val adjustment: Duration) : TimerIntent
    data object ResetTimer : TimerIntent
    data object ConfirmReset : TimerIntent
    data object CancelReset : TimerIntent
    data object StopTimer : TimerIntent
    data object ConfirmStop : TimerIntent
    data object CancelStop : TimerIntent
    data class RatingChanged(val rating: Float) : TimerIntent
    data class NotesChanged(val notes: String) : TimerIntent
    data class PhotoSelected(val imageBytes: ByteArray) : TimerIntent
    data class PhotoRemoved(val path: String) : TimerIntent
    data object ShowNextSteepDialog : TimerIntent
    data object CancelNextSteepDialog : TimerIntent
    data class UpdateNextSteepDuration(val duration: Duration?) : TimerIntent
    data class UpdateNextSteepTemperature(val temperature: Int) : TimerIntent
    data object ToggleTemperatureUnit : TimerIntent
    data object ToggleVolumeUnit : TimerIntent
    data class ConfirmNextSteep(val session: TeaSession) : TimerIntent
    data class SaveAndFinish(val session: TeaSession) : TimerIntent
    data object RestartTimer : TimerIntent
    data object BackClicked : TimerIntent

    // Configuration save dialog
    data class SaveConfigurationClicked(val customLabel: String?) : TimerIntent
    data object SkipSaveConfiguration : TimerIntent

    // Discard session
    data object DiscardSession : TimerIntent
    data object ConfirmDiscardSession : TimerIntent
    data object CancelDiscardSession : TimerIntent

    // Edit session parameters
    data object EditSession : TimerIntent
    data class EditTemperatureChanged(val value: String) : TimerIntent
    data class EditWaterQuantityChanged(val value: String) : TimerIntent
    data class EditTeaQuantityChanged(val value: String) : TimerIntent
    data class EditWaterTypeChanged(val waterType: WaterType) : TimerIntent
    data object ConfirmEditSession : TimerIntent
    data object CancelEditSession : TimerIntent
}