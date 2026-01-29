package dev.jketterer.leaflog.presentation.ui.screens.history

import kotlinx.datetime.LocalDate

sealed interface HistoryIntent {
    data object LoadData : HistoryIntent

    // Search and filter
    data class SearchQueryChanged(val query: String) : HistoryIntent
    data object ShowFilterSheet : HistoryIntent
    data object HideFilterSheet : HistoryIntent
    data class FilterByTeaType(val teaTypeId: String?) : HistoryIntent
    data class FilterByTea(val teaId: String?) : HistoryIntent
    data class FilterByDateRange(val start: LocalDate?, val end: LocalDate?) : HistoryIntent
    data class FilterByMinRating(val minRating: Float?) : HistoryIntent
    data class ToggleShowDraftsOnly(val draftsOnly: Boolean) : HistoryIntent
    data object ClearFilters : HistoryIntent

    // Actions
    data class SessionClicked(val sessionId: String) : HistoryIntent
    data class DeleteSession(val sessionId: String) : HistoryIntent
    data class BrewAgain(val sessionId: String) : HistoryIntent
    data object ClearError : HistoryIntent

    // Draft completion
    data class CompleteDraft(val sessionId: String) : HistoryIntent
    data class ConfirmCompleteDraft(
        val sessionId: String,
        val rating: Float?,
        val notes: String?,
    ) : HistoryIntent

    data object CancelCompleteDraft : HistoryIntent
}