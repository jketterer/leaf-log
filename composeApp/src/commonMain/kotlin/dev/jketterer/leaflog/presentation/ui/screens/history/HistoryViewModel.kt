package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.session.BrewAgainUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

class HistoryViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val brewAgainUseCase: BrewAgainUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val _navEvents = Channel<HistoryNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    private var loadDataJob: Job? = null

    init {
        onIntent(HistoryIntent.LoadData)
    }

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.LoadData -> loadData()
            is HistoryIntent.SearchQueryChanged -> updateSearchQuery(intent.query)
            is HistoryIntent.ShowFilterSheet -> showFilterSheet()
            is HistoryIntent.HideFilterSheet -> hideFilterSheet()
            is HistoryIntent.FilterByTeaType -> filterByTeaType(intent.teaTypeId)
            is HistoryIntent.FilterByTea -> filterByTea(intent.teaId)
            is HistoryIntent.FilterByDateRange -> filterByDateRange(intent.start, intent.end)
            is HistoryIntent.FilterByMinRating -> filterByMinRating(intent.minRating)
            is HistoryIntent.ClearFilters -> clearFilters()
            is HistoryIntent.DeleteSession -> _state.update { it.copy(sessionPendingDelete = intent.sessionId) }
            is HistoryIntent.ConfirmDeleteSession -> confirmDeleteSession()
            is HistoryIntent.CancelDeleteSession -> _state.update { it.copy(sessionPendingDelete = null) }
            is HistoryIntent.BrewAgain -> brewAgain(intent.sessionId)
            is HistoryIntent.CompleteInProgress -> _navEvents.trySend(
                HistoryNavEvent.NavigateToTimer(
                    intent.sessionId
                )
            )

            is HistoryIntent.ClearError -> clearError()

            is HistoryIntent.SessionClicked -> _navEvents.trySend(
                HistoryNavEvent.NavigateToSession(
                    intent.sessionId
                )
            )

            is HistoryIntent.EditSession -> _navEvents.trySend(
                HistoryNavEvent.NavigateToEditSession(intent.sessionId)
            )
        }
    }

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val sessionsFlow = if (_state.value.showInProgressOnly) {
                    teaSessionRepository.getInProgressFlow()
                } else {
                    teaSessionRepository.getAllFlow()
                }

                combine(
                    sessionsFlow,
                    teaRepository.getAllFlow(),
                    teaTypeRepository.getAllFlow(),
                    brewingVesselRepository.getAllFlow(),
                    preferencesRepository.getPreferencesFlow(),
                ) { sessions, teas, types, vessels, prefs ->
                    HistoryData(
                        sessions,
                        teas,
                        types,
                        vessels,
                        prefs
                    )
                }
                    .catch { e ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load data: ${e.message}",
                            )
                        }
                    }
                    .collect { (sessions, teas, types, vessels, prefs) ->
                        val teasMap = teas.associateBy { tea -> tea.id }
                        val filteredSessions = applyFilters(sessions, teasMap)

                        _state.update {
                            it.copy(
                                allSessions = sessions,
                                groupedSessions = filteredSessions.groupByTimePeriod(),
                                teas = teasMap,
                                teaTypes = types.associateBy { type -> type.id },
                                vessels = vessels.associateBy { vessel -> vessel.id },
                                userPreferences = prefs,
                                isLoading = false,
                                isEmpty = filteredSessions.isEmpty(),
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load data: ${e.message}"
                    )
                }
            }
        }
    }

    private fun applyFilters(
        sessions: List<TeaSession>,
        teasMap: Map<String, Tea>,
    ): List<TeaSession> {
        var filtered = sessions

        val currentState = _state.value

        // Filter by tea type
        if (currentState.selectedTeaTypeId != null) {
            filtered = filtered.filter { session ->
                teasMap[session.teaId]?.teaTypeId == currentState.selectedTeaTypeId
            }
        }

        // Filter by specific tea
        if (currentState.selectedTeaId != null) {
            filtered = filtered.filter { it.teaId == currentState.selectedTeaId }
        }

        // Filter by date range
        if (currentState.dateRangeStart != null || currentState.dateRangeEnd != null) {
            filtered = filtered.filter { session ->
                val sessionDate = session.timestamp.toLocalDateTime(
                    kotlinx.datetime.TimeZone.currentSystemDefault()
                ).date

                val afterStart = currentState.dateRangeStart?.let { sessionDate >= it } ?: true
                val beforeEnd = currentState.dateRangeEnd?.let { sessionDate <= it } ?: true

                afterStart && beforeEnd
            }
        }

        // Filter by minimum rating
        if (currentState.minRating != null) {
            filtered = filtered.filter { session ->
                session.rating != null && session.rating >= currentState.minRating
            }
        }

        // Filter by search query
        if (currentState.searchQuery.isNotBlank()) {
            val query = currentState.searchQuery.lowercase()
            filtered = filtered.filter { session ->
                val teaName = teasMap[session.teaId]?.name?.lowercase() ?: ""
                val notes = session.notes?.lowercase() ?: ""
                teaName.contains(query) || notes.contains(query)
            }
        }

        return filtered
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        reapplyFilters()
    }

    /**
     * Reapplies filters without showing loading indicator.
     * Used for search and filter changes where we're just filtering in-memory data.
     */
    private fun reapplyFilters() {
        val currentState = _state.value
        val filteredSessions = applyFilters(currentState.allSessions, currentState.teas)
        _state.update {
            it.copy(
                groupedSessions = filteredSessions.groupByTimePeriod(),
                isEmpty = filteredSessions.isEmpty(),
            )
        }
    }

    private fun showFilterSheet() {
        _state.update { it.copy(showFilterSheet = true) }
    }

    private fun hideFilterSheet() {
        _state.update { it.copy(showFilterSheet = false) }
    }

    private fun filterByTeaType(teaTypeId: String?) {
        _state.update { it.copy(selectedTeaTypeId = teaTypeId) }
        reapplyFilters()
    }

    private fun filterByTea(teaId: String?) {
        _state.update { it.copy(selectedTeaId = teaId) }
        reapplyFilters()
    }

    private fun filterByDateRange(start: LocalDate?, end: LocalDate?) {
        _state.update {
            it.copy(
                dateRangeStart = start,
                dateRangeEnd = end
            )
        }
        reapplyFilters()
    }

    private fun filterByMinRating(minRating: Float?) {
        _state.update { it.copy(minRating = minRating) }
        reapplyFilters()
    }

    private fun clearFilters() {
        val wasShowingInProgressOnly = _state.value.showInProgressOnly
        _state.update {
            it.copy(
                searchQuery = "",
                selectedTeaTypeId = null,
                selectedTeaId = null,
                dateRangeStart = null,
                dateRangeEnd = null,
                minRating = null,
                showInProgressOnly = false,
            )
        }
        // Only reload if we were showing in-progress (data source changes)
        if (wasShowingInProgressOnly) {
            loadData()
        } else {
            reapplyFilters()
        }
    }

    private fun confirmDeleteSession() {
        viewModelScope.launch {
            val sessionId = _state.value.sessionPendingDelete ?: return@launch
            _state.update { it.copy(sessionPendingDelete = null) }
            deleteSessionUseCase(sessionId)
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to delete session: ${e.message}") }
                }
        }
    }

    private fun brewAgain(sessionId: String) {
        viewModelScope.launch {
            // Get the session first
            val session = teaSessionRepository.getById(sessionId)
            if (session == null) {
                _state.update {
                    it.copy(error = "Session not found")
                }
                return@launch
            }

            brewAgainUseCase(session)
                .onSuccess { newSession ->
                    _navEvents.trySend(HistoryNavEvent.NavigateToTimer(newSession.id))
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to create new session: ${e.message}") }
                }
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

sealed interface HistoryNavEvent {
    data class NavigateToSession(val sessionId: String) : HistoryNavEvent
    data class NavigateToEditSession(val sessionId: String) : HistoryNavEvent
    data class NavigateToTimer(val sessionId: String) : HistoryNavEvent
}

private data class HistoryData(
    val sessions: List<TeaSession>,
    val teas: List<Tea>,
    val types: List<TeaType>,
    val vessels: List<BrewingVessel>,
    val prefs: UserPreferences,
)
