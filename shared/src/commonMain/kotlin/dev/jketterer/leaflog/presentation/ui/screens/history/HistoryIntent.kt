package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.presentation.ui.viewmodel.BrewAgainIntent

sealed interface HistoryIntent {
    data object LoadData : HistoryIntent

    // Search and filter
    data class SearchQueryChanged(val query: String) : HistoryIntent
    data object ShowFilterSheet : HistoryIntent
    data object HideFilterSheet : HistoryIntent
    data class FiltersChanged(val filters: HistoryFilters) : HistoryIntent

    /** Clears the search too. */
    data object ClearFilters : HistoryIntent

    // Actions
    data class SessionClicked(val sessionId: String) : HistoryIntent
    data class DeleteSession(val sessionId: String) : HistoryIntent
    data object ConfirmDeleteSession : HistoryIntent
    data object CancelDeleteSession : HistoryIntent
    data class BrewAgainClicked(val sessionId: String) : HistoryIntent
    data class BrewAgain(val intent: BrewAgainIntent) : HistoryIntent

    // In-progress completion - navigates to timer screen
    data class CompleteInProgress(val sessionId: String) : HistoryIntent

    // In-progress session conflict dialog
    data object ResumeInProgress : HistoryIntent
    data object DismissInProgressDialog : HistoryIntent
    data object CompleteInProgressAndContinue : HistoryIntent
    data object DiscardInProgressAndContinue : HistoryIntent
}
