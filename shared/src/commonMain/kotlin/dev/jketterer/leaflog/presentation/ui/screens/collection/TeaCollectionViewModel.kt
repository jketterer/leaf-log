package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.SearchTeasUseCase
import dev.jketterer.leaflog.domain.usecases.ToggleFavoriteUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_teas
import leaf_log.shared.generated.resources.couldnt_search
import leaf_log.shared.generated.resources.couldnt_update_favorite

class TeaCollectionViewModel(
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val searchTeaUseCase: SearchTeasUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeaCollectionState())
    val state: StateFlow<TeaCollectionState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

    private var teaCollectionJob: Job? = null

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.getPreferences()
            _state.update { it.copy(selectedSortOption = prefs.teaSortOption) }
            onIntent(TeaCollectionIntent.LoadData)
        }
    }

    fun onIntent(intent: TeaCollectionIntent) {
        when (intent) {
            is TeaCollectionIntent.LoadData -> loadData()
            is TeaCollectionIntent.ShowAllTeas -> showByFilter(FilterType.ALL)
            is TeaCollectionIntent.ShowFavorites -> showByFilter(FilterType.FAVORITES)
            is TeaCollectionIntent.FilterByType -> showByFilter(
                filterType = FilterType.BY_TYPE,
                teaTypeId = intent.teaTypeId
            )

            is TeaCollectionIntent.SearchQueryChanged -> updateSearchQuery(intent.query)
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

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                collectTeaTypes()
                collectTeas()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_teas)
            }
        }
    }

    private fun collectTeaTypes() = viewModelScope.launch {
        teaTypeRepository.getAllFlow()
            .catchError()
            .collect { teaTypes ->
                _state.update { it.copy(teaTypes = teaTypes) }
            }
    }

    private fun collectTeas() {
        teaCollectionJob?.cancel()
        teaCollectionJob = viewModelScope.launch {
            val teasFlow = when (_state.value.selectedFilter) {
                FilterType.ALL -> teaRepository.getAllFlow()
                FilterType.FAVORITES -> teaRepository.getFavoritesFlow()
                FilterType.BY_TYPE -> _state.value.selectedTypeId?.let { typeId ->
                    teaRepository.getByTypeFlow(typeId)
                } ?: teaRepository.getAllFlow()
            }

            teasFlow
                .catchError()
                .collect { teas ->
                    val sorted = sortTeas(teas, _state.value.selectedSortOption)
                    _state.update {
                        it.copy(
                            teas = sorted,
                            isLoading = false,
                            isEmpty = teas.isEmpty(),
                        )
                    }
                }
        }
    }

    private fun <T> Flow<T>.catchError(): Flow<T> {
        return catch { e ->
            _state.update { it.copy(isLoading = false) }
            messages.failure(e, Res.string.couldnt_load_teas)
        }
    }

    private fun showByFilter(filterType: FilterType, teaTypeId: String? = null) {
        _state.update {
            it.copy(
                selectedFilter = filterType,
                selectedTypeId = teaTypeId,
                searchQuery = "",
            )
        }
        loadData()
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            loadData()
            return
        }

        teaCollectionJob?.cancel()
        viewModelScope.launch {
            try {
                val results = searchTeaUseCase(query)
                val sorted = sortTeas(results, _state.value.selectedSortOption)
                _state.update {
                    it.copy(
                        teas = sorted,
                        isEmpty = results.isEmpty(),
                    )
                }
            } catch (e: Exception) {
                messages.failure(e, Res.string.couldnt_search)
            }
        }
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
        _state.update {
            it.copy(
                selectedSortOption = option,
                teas = sortTeas(it.teas, option),
            )
        }
        viewModelScope.launch {
            preferencesRepository.updateTeaSortOption(option)
        }
    }
}

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
