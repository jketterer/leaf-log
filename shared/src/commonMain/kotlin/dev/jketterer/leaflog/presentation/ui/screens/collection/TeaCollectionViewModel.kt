package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.models.isDormant
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.search.matchesQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_teas
import leaf_log.shared.generated.resources.couldnt_update_favorite
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class TeaCollectionViewModel(
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeaCollectionState())
    val state: StateFlow<TeaCollectionState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.getPreferences()
            _state.update { it.copy(selectedSortOption = prefs.teaSortOption) }
            loadData()
        }
    }

    fun onIntent(intent: TeaCollectionIntent) {
        when (intent) {
            is TeaCollectionIntent.ShowAllTeas -> showByFilter(FilterType.ALL)
            is TeaCollectionIntent.ShowFavorites -> showByFilter(FilterType.FAVORITES)
            is TeaCollectionIntent.FilterByType -> showByFilter(
                filterType = FilterType.BY_TYPE,
                teaTypeId = intent.teaTypeId
            )

            is TeaCollectionIntent.ToggleNotBrewedLately ->
                _state.update { it.copy(notBrewedLately = !it.notBrewedLately) }

            is TeaCollectionIntent.SearchQueryChanged -> _state.update { it.copy(searchQuery = intent.query) }
            is TeaCollectionIntent.ToggleFavorite -> toggleFavorite(intent.tea)
            is TeaCollectionIntent.TabSelected -> selectTab(intent.tab)
            is TeaCollectionIntent.TeaClicked -> {
                // navigation handled by UI
            }

            is TeaCollectionIntent.AddTeaClicked -> {
                // navigation handled by UI
            }

            is TeaCollectionIntent.SortSelected -> selectSort(intent.option)
        }
    }

    @OptIn(FlowPreview::class)
    private fun loadData() {
        _state.update { it.copy(isLoading = true) }

        teaTypeRepository.getAllFlow()
            .onEach { teaTypes -> _state.update { it.copy(teaTypes = teaTypes) } }
            .catchError()
            .launchIn(viewModelScope)

        val criteria = _state.map { Triple(it.selectedFilter, it.selectedTypeId, it.selectedSortOption) }
            .distinctUntilChanged()
        val notBrewedLately = _state.map { it.notBrewedLately }.distinctUntilChanged()
        // Typing settles before the search runs; clearing it shows everything at once
        val query = _state.map { it.searchQuery }.distinctUntilChanged()
            .debounce { if (it.isBlank()) Duration.ZERO else SEARCH_DEBOUNCE }

        // Filtering, searching and sorting read every tea, so they run off Main
        combine(
            teaRepository.getAllFlow(),
            criteria,
            query,
            notBrewedLately,
        ) { teas, (filter, typeId, sort), query, notBrewedLately ->
            val filtered = teas.filteredBy(filter, typeId, query)
            val shown = if (notBrewedLately) {
                dormantTeas(filtered, Clock.System.now())
            } else {
                sortTeas(filtered, sort)
            }
            _state.update {
                it.copy(
                    teas = shown,
                    isLoading = false,
                    isEmpty = shown.isEmpty(),
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .catchError()
            .launchIn(viewModelScope)
    }

    private fun <T> Flow<T>.catchError(): Flow<T> {
        return catch { e ->
            _state.update { it.copy(isLoading = false) }
            messages.failure(e, Res.string.couldnt_load_teas)
        }
    }

    private fun showByFilter(filterType: FilterType, teaTypeId: String? = null) {
        _state.update { it.copy(selectedFilter = filterType, selectedTypeId = teaTypeId) }
    }

    private fun toggleFavorite(tea: Tea) {
        viewModelScope.launch {
            toggleFavoriteUseCase(tea)
                .onFailure { messages.failure(it, Res.string.couldnt_update_favorite) }
        }
    }

    private fun selectTab(tab: CollectionTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    private fun selectSort(option: TeaSortOption) {
        _state.update { it.copy(selectedSortOption = option) }
        viewModelScope.launch {
            preferencesRepository.updateTeaSortOption(option)
        }
    }
}

/** The teas passing [filter] whose name, producer, origin or description match [query]. */
internal fun List<Tea>.filteredBy(filter: FilterType, typeId: String?, query: String): List<Tea> =
    filter { tea ->
        val passesFilter = when (filter) {
            FilterType.ALL -> true
            FilterType.FAVORITES -> tea.isFavorite
            FilterType.BY_TYPE -> typeId == null || tea.teaTypeId == typeId
        }
        passesFilter && matchesQuery(query, listOf(tea.name, tea.producer, tea.origin, tea.description))
    }

/** The dormant [teas], longest unbrewed first, whatever the chosen sort. */
internal fun dormantTeas(teas: List<Tea>, now: Instant): List<Tea> =
    teas.filter { it.isDormant(now) }.sortedBy { it.lastBrewedAt }

private val SEARCH_DEBOUNCE = 300.milliseconds

/** Sorts by [option], with finished teas after the rest in the same order. */
internal fun sortTeas(teas: List<Tea>, option: TeaSortOption): List<Tea> {
    val sorted = when (option) {
        TeaSortOption.NAME_ASC -> teas.sortedBy { it.name.lowercase() }
        TeaSortOption.RATING_DESC -> teas.sortedByDescending { it.averageRating }
        TeaSortOption.TIMES_BREWED_DESC -> teas.sortedByDescending { it.totalSessions }
        TeaSortOption.MOST_RECENT -> teas.sortedByDescending { it.lastBrewedAt }
        TeaSortOption.OLDEST -> teas.sortedBy { it.createdAt }
    }
    // sortedBy is stable
    return sorted.sortedBy { it.finishedAt != null }
}
