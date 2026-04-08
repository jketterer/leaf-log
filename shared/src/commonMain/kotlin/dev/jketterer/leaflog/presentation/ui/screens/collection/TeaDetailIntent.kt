package dev.jketterer.leaflog.presentation.ui.screens.collection

sealed interface TeaDetailIntent {
    data class LoadTea(val teaId: String) : TeaDetailIntent
    data object EditTeaClicked : TeaDetailIntent
    data object DeleteTeaClicked : TeaDetailIntent
    data object ConfirmDelete : TeaDetailIntent
    data object CancelDelete : TeaDetailIntent
    data object ToggleFavorite : TeaDetailIntent
    data class SessionClicked(val sessionId: String) : TeaDetailIntent
    data class DeleteSessionClicked(val sessionId: String) : TeaDetailIntent
    data object ConfirmDeleteSession : TeaDetailIntent
    data object CancelDeleteSession : TeaDetailIntent
    data class BrewThisTeaClicked(val vesselId: String?) : TeaDetailIntent
    data class BrewAgainClicked(val sessionId: String) : TeaDetailIntent
    data object ViewAllSessionsClicked : TeaDetailIntent
    data object BackClicked : TeaDetailIntent

    // Configuration management
    data class EditConfigurationClicked(val configId: String) : TeaDetailIntent
    data class DeleteConfigurationClicked(val configId: String) : TeaDetailIntent
    data object ConfirmDeleteConfiguration : TeaDetailIntent
    data object CancelDeleteConfiguration : TeaDetailIntent

    data object AddConfigurationClicked : TeaDetailIntent
    data object ClearError : TeaDetailIntent

    // In-progress session conflict dialog
    data object ResumeInProgress : TeaDetailIntent
    data object DismissInProgressDialog : TeaDetailIntent
    data object CompleteInProgressAndContinue : TeaDetailIntent
    data object DiscardInProgressAndContinue : TeaDetailIntent
}