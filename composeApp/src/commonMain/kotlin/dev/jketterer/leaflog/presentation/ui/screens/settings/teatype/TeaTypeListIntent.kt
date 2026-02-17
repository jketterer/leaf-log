package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

sealed interface TeaTypeListIntent {
    data class TeaTypeClicked(val teaTypeId: String) : TeaTypeListIntent
    data object AddClicked : TeaTypeListIntent
    data object BackClicked : TeaTypeListIntent
    data object ClearError : TeaTypeListIntent
}
