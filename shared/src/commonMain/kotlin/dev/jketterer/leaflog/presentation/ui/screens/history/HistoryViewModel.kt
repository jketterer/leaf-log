package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.FinishSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.viewmodel.createInProgressSessionDelegate
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
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_delete_session
import leaf_log.shared.generated.resources.couldnt_load_sessions
import leaf_log.shared.generated.resources.couldnt_start_session
import leaf_log.shared.generated.resources.failure_not_found
import kotlinx.datetime.toLocalDateTime

class HistoryViewModel(
    route: NavRoute.HistoryRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val tastingTagRepository: TastingTagRepository,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val discardSteepUseCase: DiscardSteepUseCase,
    private val timerService: TimerService,
    private val getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(initialHistoryState(route))
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val _navEvents = Channel<HistoryNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    private var loadDataJob: Job? = null

    private sealed interface PendingAction {
        data class BrewAgain(val sessionId: String) : PendingAction
    }

    private val inProgressDelegate = createInProgressSessionDelegate<HistoryState, PendingAction>(
        getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
        finishSessionUseCase = finishSessionUseCase,
        discardSteepUseCase = discardSteepUseCase,
        timerService = timerService,
        stateFlow = _state,
        getDialogState = { it.inProgressDialogState },
        setDialogState = { state, dialogState -> state.copy(inProgressDialogState = dialogState) },
        onFailure = messages::failure,
        onResume = { session ->
            _navEvents.trySend(HistoryNavEvent.NavigateToTimer(session.session.id))
        },
        executePendingAction = { action ->
            when (action) {
                is PendingAction.BrewAgain -> brewAgain(action.sessionId)
            }
        },
    )

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
            is HistoryIntent.FilterByVessel -> filterByVessel(intent.vesselId)
            is HistoryIntent.ClearFilters -> clearFilters()
            is HistoryIntent.DeleteSession -> _state.update { it.copy(sessionPendingDelete = intent.sessionId) }
            is HistoryIntent.ConfirmDeleteSession -> confirmDeleteSession()
            is HistoryIntent.CancelDeleteSession -> _state.update { it.copy(sessionPendingDelete = null) }
            is HistoryIntent.BrewAgain -> inProgressDelegate.checkAndProceed(PendingAction.BrewAgain(intent.sessionId))
            is HistoryIntent.CompleteInProgress -> _navEvents.trySend(
                HistoryNavEvent.NavigateToTimer(
                    intent.sessionId
                )
            )

            // In-progress session conflict dialog
            is HistoryIntent.ResumeInProgress -> inProgressDelegate.resume()
            is HistoryIntent.DismissInProgressDialog -> inProgressDelegate.dismissDialog()
            is HistoryIntent.CompleteInProgressAndContinue -> inProgressDelegate.completeAndContinue()
            is HistoryIntent.DiscardInProgressAndContinue -> inProgressDelegate.discardAndContinue()

            is HistoryIntent.SessionClicked -> _navEvents.trySend(
                HistoryNavEvent.NavigateToSession(
                    intent.sessionId
                )
            )
        }
    }

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                combine(
                    teaSessionRepository.getAllSessionsFlow(),
                    teaRepository.getAllFlow(),
                    teaTypeRepository.getAllFlow(),
                    brewingVesselRepository.getAllFlow(),
                    ::HistoryData,
                )
                    .catch { e ->
                        _state.update { it.copy(isLoading = false) }
                        messages.failure(e, Res.string.couldnt_load_sessions)
                    }
                    .collect { (sessions, teas, types, vessels) ->
                        val teasMap = teas.associateBy { tea -> tea.id }
                        val filteredSessions = applyFilters(sessions, teasMap)
                        // Read again on each emission, so a tag made since shows by name
                        val vocabulary = tastingTagRepository.getAll()

                        _state.update {
                            it.copy(
                                allSessions = sessions,
                                groupedSessions = filteredSessions.groupByTimePeriod(),
                                teas = teasMap,
                                teaTypes = types.associateBy { type -> type.id },
                                vessels = vessels.associateBy { vessel -> vessel.id },
                                tagVocabulary = vocabulary,
                                isLoading = false,
                                isEmpty = filteredSessions.isEmpty(),
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_sessions)
            }
        }
    }

    private fun applyFilters(
        sessions: List<SessionWithSteeps>,
        teasMap: Map<String, Tea>,
    ): List<SessionWithSteeps> {
        var filtered = sessions

        val currentState = _state.value

        // Filter by tea type
        if (currentState.selectedTeaTypeId != null) {
            filtered = filtered.filter { session ->
                teasMap[session.session.teaId]?.teaTypeId == currentState.selectedTeaTypeId
            }
        }

        // Filter by specific tea
        if (currentState.selectedTeaId != null) {
            filtered = filtered.filter { it.session.teaId == currentState.selectedTeaId }
        }

        // Filter by date range
        if (currentState.dateRangeStart != null || currentState.dateRangeEnd != null) {
            filtered = filtered.filter { session ->
                val sessionDate = session.startedAt.toLocalDateTime(
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
                session.effectiveRating?.let { it >= currentState.minRating } ?: false
            }
        }

        // Filter by vessel
        if (currentState.selectedVesselId != null) {
            filtered = filtered.filter { it.session.vesselId == currentState.selectedVesselId }
        }

        // Filter by search query
        if (currentState.searchQuery.isNotBlank()) {
            val query = currentState.searchQuery.lowercase()
            filtered = filtered.filter { session ->
                val teaName = teasMap[session.session.teaId]?.name?.lowercase() ?: ""
                teaName.contains(query) ||
                    session.session.notes?.lowercase()?.contains(query) == true ||
                    session.steeps.any { it.notes?.lowercase()?.contains(query) == true }
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

    private fun filterByVessel(vesselId: String?) {
        _state.update { it.copy(selectedVesselId = vesselId) }
        reapplyFilters()
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
                selectedVesselId = null,
            )
        }
        reapplyFilters()
    }

    private fun confirmDeleteSession() {
        viewModelScope.launch {
            val sessionId = _state.value.sessionPendingDelete ?: return@launch
            _state.update { it.copy(sessionPendingDelete = null) }
            deleteSessionUseCase(sessionId)
                .onSuccess { timerService.onSessionResolved(sessionId) }
                .onFailure { messages.failure(it, Res.string.couldnt_delete_session) }
        }
    }

    private fun brewAgain(sessionId: String) {
        viewModelScope.launch {
            val session = teaSessionRepository.getSession(sessionId)
            if (session == null) {
                messages.show(Res.string.failure_not_found)
                return@launch
            }

            startSessionUseCase(session)
                .onSuccess { newSession ->
                    _navEvents.trySend(HistoryNavEvent.NavigateToTimer(newSession.session.id))
                }
                .onFailure { messages.failure(it, Res.string.couldnt_start_session) }
        }
    }
}

/** Opened from elsewhere, History starts filtered the way the route asks. */
internal fun initialHistoryState(route: NavRoute.HistoryRoute) = HistoryState(
    selectedTeaTypeId = route.filterTeaTypeId,
    selectedTeaId = route.filterTeaId,
    dateRangeStart = route.filterDateStart?.let(LocalDate::parse),
    dateRangeEnd = route.filterDateEnd?.let(LocalDate::parse),
)

sealed interface HistoryNavEvent {
    data class NavigateToSession(val sessionId: String) : HistoryNavEvent
    data class NavigateToTimer(val sessionId: String) : HistoryNavEvent
}

private data class HistoryData(
    val sessions: List<SessionWithSteeps>,
    val teas: List<Tea>,
    val types: List<TeaType>,
    val vessels: List<BrewingVessel>,
)
