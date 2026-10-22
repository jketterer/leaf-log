package dev.jketterer.leaflog.presentation.ui.screens.history

import kotlin.time.Duration

sealed interface SessionDetailIntent {
    data object EditSessionClicked : SessionDetailIntent
    data object DeleteSessionClicked : SessionDetailIntent
    data object ConfirmDelete : SessionDetailIntent
    data object CancelDelete : SessionDetailIntent
    data class DeleteSteepClicked(val steepId: String) : SessionDetailIntent
    data object ConfirmDeleteSteep : SessionDetailIntent
    data object CancelDeleteSteep : SessionDetailIntent
    data object BrewAgainClicked : SessionDetailIntent
    data object AddSteepClicked : SessionDetailIntent
    data object CancelAddSteep : SessionDetailIntent
    data class UpdateNextSteepDuration(val duration: Duration?) : SessionDetailIntent
    data class UpdateNextSteepTemperature(val temperature: Double?) : SessionDetailIntent
    data object ConfirmAddSteep : SessionDetailIntent
    data class ViewTeaClicked(val teaId: String) : SessionDetailIntent
    data object BackClicked : SessionDetailIntent
    data class StepToSession(val sessionId: String) : SessionDetailIntent
    data object SaveAsConfigurationClicked : SessionDetailIntent
    data class ConfirmSaveConfiguration(val label: String) : SessionDetailIntent
    data object DismissSaveConfiguration : SessionDetailIntent

    // The steep sheet; each edit saves as it's made
    data class SteepClicked(val steepId: String) : SessionDetailIntent
    data class SteepRatingChanged(val rating: Float) : SessionDetailIntent
    data class SteepNotesChanged(val notes: String) : SessionDetailIntent
    data class SteepTagToggled(val tagId: String) : SessionDetailIntent
    data class SteepTagCreated(val name: String) : SessionDetailIntent
    data class SteepParametersChanged(
        val brewedTime: Duration,
        val temperatureCelsius: Double,
        val waterQuantityMl: Double,
    ) : SessionDetailIntent
    data object DismissSteepEdit : SessionDetailIntent

    // In-progress session conflict dialog
    data object ResumeInProgress : SessionDetailIntent
    data object DismissInProgressDialog : SessionDetailIntent
    data object CompleteInProgressAndContinue : SessionDetailIntent
    data object DiscardInProgressAndContinue : SessionDetailIntent
}
