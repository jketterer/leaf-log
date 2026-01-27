package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.Tea

sealed interface TeaCollectionIntent {
    data object LoadData : TeaCollectionIntent
    data object ShowAllTeas : TeaCollectionIntent
    data object ShowFavorites : TeaCollectionIntent
    data class FilterByType(val teaTypeId: String) : TeaCollectionIntent
    data class SearchQueryChanged(val query: String) : TeaCollectionIntent
    data class TeaClicked(val teaId: String) : TeaCollectionIntent
    data class ToggleFavorite(val tea: Tea) : TeaCollectionIntent
    data object AddTeaClicked : TeaCollectionIntent
    data object ClearError : TeaCollectionIntent
}