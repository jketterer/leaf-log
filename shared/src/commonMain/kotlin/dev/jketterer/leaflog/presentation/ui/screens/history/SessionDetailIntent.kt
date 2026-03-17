package dev.jketterer.leaflog.presentation.ui.screens.history

import kotlin.time.Duration

sealed interface SessionDetailIntent {
    data class LoadSession(val sessionId: String) : SessionDetailIntent
    data object EditSessionClicked : SessionDetailIntent
    data class EditSteepClicked(val steepId: String) : SessionDetailIntent
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
    data class UpdateNextSteepTemperature(val temperature: Double) : SessionDetailIntent
    data object ToggleTemperatureUnit : SessionDetailIntent
    data object ConfirmAddSteep : SessionDetailIntent
    data class ViewTeaClicked(val teaId: String) : SessionDetailIntent
    data object BackClicked : SessionDetailIntent
    data object ClearError : SessionDetailIntent
    data object SaveAsConfigurationClicked : SessionDetailIntent
    data class ConfirmSaveConfiguration(val label: String) : SessionDetailIntent
    data object DismissSaveConfiguration : SessionDetailIntent
}