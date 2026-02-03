package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
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
import dev.jketterer.leaflog.domain.usecases.session.CompleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
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
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val brewAgainUseCase: BrewAgainUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val _navEvents = Channel<HistoryNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

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
            is HistoryIntent.ToggleShowDraftsOnly -> toggleShowDraftsOnly(intent.draftsOnly)
            is HistoryIntent.ClearFilters -> clearFilters()
            is HistoryIntent.DeleteSession -> deleteSession(intent.sessionId)
            is HistoryIntent.BrewAgain -> brewAgain(intent.sessionId)
            is HistoryIntent.CompleteDraft -> showCompleteDraftDialog(intent.sessionId)
            is HistoryIntent.ConfirmCompleteDraft -> completeDraftSession(
                intent.sessionId,
                intent.rating,
                intent.notes,
            )

            is HistoryIntent.CancelCompleteDraft -> hideCompleteDraftDialog()
            is HistoryIntent.ClearError -> clearError()

            is HistoryIntent.SessionClicked -> _navEvents.trySend(
                HistoryNavEvent.NavigateToSession(
                    intent.sessionId
                )
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val sessionsFlow = if (_state.value.showDraftsOnly) {
                    teaSessionRepository.getDraftsFlow()
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
                        val filteredSessions = applyFilters(sessions)

                        _state.update {
                            it.copy(
                                sessions = filteredSessions,
                                groupedSessions = filteredSessions.groupByTimePeriod(),
                                teas = teas.associateBy { tea -> tea.id },
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

    private fun applyFilters(sessions: List<TeaSession>): List<TeaSession> {
        var filtered = sessions

        val currentState = _state.value

        // Filter by tea type
        if (currentState.selectedTeaTypeId != null) {
            filtered = filtered.filter { session ->
                currentState.teas[session.teaId]?.teaTypeId == currentState.selectedTeaTypeId
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
                val teaName = currentState.teas[session.teaId]?.name?.lowercase() ?: ""
                val notes = session.notes?.lowercase() ?: ""
                teaName.contains(query) || notes.contains(query)
            }
        }

        return filtered
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        loadData()  // Reapply filters
    }

    private fun showFilterSheet() {
        _state.update { it.copy(showFilterSheet = true) }
    }

    private fun hideFilterSheet() {
        _state.update { it.copy(showFilterSheet = false) }
    }

    private fun filterByTeaType(teaTypeId: String?) {
        _state.update { it.copy(selectedTeaTypeId = teaTypeId) }
        loadData()
    }

    private fun filterByTea(teaId: String?) {
        _state.update { it.copy(selectedTeaId = teaId) }
        loadData()
    }

    private fun filterByDateRange(start: LocalDate?, end: LocalDate?) {
        _state.update {
            it.copy(
                dateRangeStart = start,
                dateRangeEnd = end
            )
        }
        loadData()
    }

    private fun filterByMinRating(minRating: Float?) {
        _state.update { it.copy(minRating = minRating) }
        loadData()
    }

    private fun toggleShowDraftsOnly(draftsOnly: Boolean) {
        _state.update { it.copy(showDraftsOnly = draftsOnly) }
        loadData()
    }

    private fun clearFilters() {
        _state.update {
            it.copy(
                searchQuery = "",
                selectedTeaTypeId = null,
                selectedTeaId = null,
                dateRangeStart = null,
                dateRangeEnd = null,
                minRating = null,
                showDraftsOnly = false,
            )
        }
        loadData()
    }

    private fun deleteSession(sessionId: String) {
        viewModelScope.launch {
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
                .onSuccess {
                    // Navigation handled by UI - navigate to Log Tea with pre-filled data
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to create new session: ${e.message}") }
                }
        }
    }

    private fun showCompleteDraftDialog(sessionId: String) {
        viewModelScope.launch {
            val session = teaSessionRepository.getById(sessionId)
            if (session != null && session.status == SessionStatus.DRAFT) {
                _state.update {
                    it.copy(
                        draftToComplete = session,
                        showCompleteDraftDialog = true,
                    )
                }
            }
        }
    }

    private fun hideCompleteDraftDialog() {
        _state.update {
            it.copy(
                draftToComplete = null,
                showCompleteDraftDialog = false,
            )
        }
    }

    private fun completeDraftSession(
        sessionId: String,
        rating: Float?,
        notes: String?,
    ) {
        viewModelScope.launch {
            val session = teaSessionRepository.getById(sessionId)
            if (session == null) {
                _state.update {
                    it.copy(
                        showCompleteDraftDialog = false,
                        error = "Session not found",
                    )
                }
                return@launch
            }

            completeSessionUseCase(
                session = session,
                rating = rating,
                finalNotes = notes,
            )
                .onSuccess {
                    _state.update {
                        it.copy(
                            draftToComplete = null,
                            showCompleteDraftDialog = false,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            showCompleteDraftDialog = false,
                            error = "Failed to complete session: ${e.message}",
                        )
                    }
                }
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

sealed interface HistoryNavEvent {
    data class NavigateToSession(val sessionId: String) : HistoryNavEvent
}

private data class HistoryData(
    val sessions: List<TeaSession>,
    val teas: List<Tea>,
    val types: List<TeaType>,
    val vessels: List<BrewingVessel>,
    val prefs: UserPreferences,
)