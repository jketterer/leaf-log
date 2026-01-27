package dev.jketterer.leaflog.presentation.ui.screens.history

sealed interface SessionDetailIntent {
    data class LoadSession(val sessionId: String) : SessionDetailIntent
    data object EditSessionClicked : SessionDetailIntent
    data class EditSteepClicked(val steepId: String) : SessionDetailIntent
    data object DeleteSessionClicked : SessionDetailIntent
    data object ConfirmDelete : SessionDetailIntent
    data object CancelDelete : SessionDetailIntent
    data class DeleteSteep(val steepId: String) : SessionDetailIntent
    data object BrewAgainClicked : SessionDetailIntent
    data object ViewTeaClicked : SessionDetailIntent
    data object BackClicked : SessionDetailIntent
}