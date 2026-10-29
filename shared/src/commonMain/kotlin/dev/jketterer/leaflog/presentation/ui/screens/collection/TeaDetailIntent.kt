package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.presentation.ui.viewmodel.BrewAgainIntent

sealed interface TeaDetailIntent {
    data object EditTeaClicked : TeaDetailIntent
    data object DeleteTeaClicked : TeaDetailIntent
    data object ConfirmDelete : TeaDetailIntent
    data object CancelDelete : TeaDetailIntent
    data object ToggleFavorite : TeaDetailIntent
    data object ToggleFinished : TeaDetailIntent
    data class SessionClicked(val sessionId: String) : TeaDetailIntent
    data class DeleteSessionClicked(val sessionId: String) : TeaDetailIntent
    data object ConfirmDeleteSession : TeaDetailIntent
    data object CancelDeleteSession : TeaDetailIntent
    data class BrewThisTeaClicked(val vesselId: String?) : TeaDetailIntent
    data class BrewAgainClicked(val sessionId: String) : TeaDetailIntent
    data class BrewAgain(val intent: BrewAgainIntent) : TeaDetailIntent
    data object ViewAllSessionsClicked : TeaDetailIntent
    data object BackClicked : TeaDetailIntent

    // In-progress session conflict dialog
    data object ResumeInProgress : TeaDetailIntent
    data object DismissInProgressDialog : TeaDetailIntent
    data object CompleteInProgressAndContinue : TeaDetailIntent
    data object DiscardInProgressAndContinue : TeaDetailIntent
}