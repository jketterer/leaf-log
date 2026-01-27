package dev.jketterer.leaflog.presentation.ui.screens.collection

sealed interface TeaDetailIntent {
    data class LoadTea(val teaId: String) : TeaDetailIntent
    data object EditTeaClicked : TeaDetailIntent
    data object DeleteTeaClicked : TeaDetailIntent
    data object ConfirmDelete : TeaDetailIntent
    data object CancelDelete : TeaDetailIntent
    data object ToggleFavorite : TeaDetailIntent
    data class SessionClicked(val sessionId: String) : TeaDetailIntent
    data object BrewThisTeaClicked : TeaDetailIntent
    data object BackClicked : TeaDetailIntent
}