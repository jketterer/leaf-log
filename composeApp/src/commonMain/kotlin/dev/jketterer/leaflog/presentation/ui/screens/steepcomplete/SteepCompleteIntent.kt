package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import dev.jketterer.leaflog.presentation.ui.screens.timer.TimerIntent
import kotlin.time.Duration

sealed interface SteepCompleteIntent {
    data class Initialize(val sessionId: String) : SteepCompleteIntent
    data class RatingChanged(val rating: Float) : SteepCompleteIntent
    data class NotesChanged(val notes: String) : SteepCompleteIntent
    data class PhotoSelected(val imageBytes: ByteArray) : SteepCompleteIntent
    data class PhotoRemoved(val path: String) : SteepCompleteIntent
    data object ShowNextSteepDialog : SteepCompleteIntent
    data object DismissNextSteepDialog : SteepCompleteIntent
    data class NextSteepDurationChanged(val duration: Duration?) : SteepCompleteIntent
    data class NextSteepTemperatureChanged(val temperature: Double) : SteepCompleteIntent
    data class NextSteepWaterQuantityChanged(val waterQuantityMl: Double) : SteepCompleteIntent
    data object ConfirmNextSteep : SteepCompleteIntent
    data object FinishSession : SteepCompleteIntent
    data class SaveConfigurationClicked(val customLabel: String?) : SteepCompleteIntent
    data object SkipSaveConfiguration : SteepCompleteIntent
    data object ShowDiscardConfirmation : SteepCompleteIntent
    data object DismissDiscardConfirmation : SteepCompleteIntent
    data object ConfirmDiscard : SteepCompleteIntent
    data class PreviousSteepRatingChanged(val steepId: String, val rating: Float) : SteepCompleteIntent
    data class PreviousSteepNotesChanged(val steepId: String, val notes: String) : SteepCompleteIntent
    data object ToggleTemperatureUnit : SteepCompleteIntent
    data object BackClicked : SteepCompleteIntent
}
