package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.TeaType

data class TeaCollectionState(
    val teas: List<Tea> = emptyList(),
    val teaTypes: List<TeaType> = emptyList(),
    val selectedFilter: FilterType = FilterType.ALL,
    val selectedTypeId: String? = null,
    val selectedTab: CollectionTab = CollectionTab.TEAS,
    val searchQuery: String = "",
    val selectedSortOption: TeaSortOption = TeaSortOption.NAME_ASC,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false,
)

enum class CollectionTab {
    TEAS,
    VESSELS,
}

enum class FilterType {
    ALL,
    FAVORITES,
    BY_TYPE,
}

