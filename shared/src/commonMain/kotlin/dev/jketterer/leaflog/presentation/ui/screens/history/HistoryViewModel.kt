package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
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
import dev.jketterer.leaflog.presentation.ui.components.session.loadLabel
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.search.matchesQuery
import dev.jketterer.leaflog.presentation.ui.viewmodel.createInProgressSessionDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_delete_session
import leaf_log.shared.generated.resources.couldnt_load_sessions
import leaf_log.shared.generated.resources.couldnt_start_session
import leaf_log.shared.generated.resources.failure_not_found
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    private val clock: Clock,
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
            is HistoryIntent.SearchQueryChanged -> _state.update { it.copy(searchQuery = intent.query) }
            is HistoryIntent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = true) }
            is HistoryIntent.HideFilterSheet -> _state.update { it.copy(showFilterSheet = false) }
            is HistoryIntent.FiltersChanged -> _state.update { it.copy(filters = intent.filters) }
            is HistoryIntent.ClearFilters -> _state.update {
                it.copy(searchQuery = "", filters = HistoryFilters())
            }

            is HistoryIntent.DeleteSession -> _state.update { it.copy(sessionPendingDelete = intent.sessionId) }
            is HistoryIntent.ConfirmDeleteSession -> confirmDeleteSession()
            is HistoryIntent.CancelDeleteSession -> _state.update { it.copy(sessionPendingDelete = null) }
            is HistoryIntent.BrewAgain -> inProgressDelegate.checkAndProceed(
                PendingAction.BrewAgain(
                    intent.sessionId
                )
            )

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

    @OptIn(FlowPreview::class)
    private fun loadData() {
        loadDataJob?.cancel()
        _state.update { it.copy(isLoading = true) }
        val data = combine(
            teaSessionRepository.getAllSessionsFlow(),
            teaRepository.getAllFlow(),
            teaTypeRepository.getAllFlow(),
            brewingVesselRepository.getAllFlow(),
            ::HistoryData,
        )
            // Read again on each emission, so a tag made since shows by name
            .map { data ->
                val vocabulary = tastingTagRepository.getAll()
                data.copy(
                    vocabulary = vocabulary,
                    tagLabels = vocabulary.associate { it.id to it.loadLabel() })
            }
        val filters = _state.map { it.filters }.distinctUntilChanged()
        // Typing settles before the search runs; clearing it shows everything at once
        val query = _state.map { it.searchQuery }.distinctUntilChanged()
            .debounce { if (it.isBlank()) Duration.ZERO else SEARCH_DEBOUNCE }

        // Filtering, searching and grouping read every session, so they run off Main
        loadDataJob = combine(data, filters, query) { data, filters, query ->
            val teasMap = data.teas.associateBy { tea -> tea.id }
            val vesselsMap = data.vessels.associateBy { vessel -> vessel.id }
            val filteredSessions = data.sessions.filteredBy(filters, teasMap).filter { session ->
                matchesQuery(query, session.searchFields(teasMap, vesselsMap, data.tagLabels))
            }
            val tz = TimeZone.currentSystemDefault()
            val groups = if (filters.isActive || query.isNotBlank()) {
                listOf(HistoryGroup(period = null, sessions = filteredSessions))
            } else {
                filteredSessions.groupByDay(clock.now().toLocalDateTime(tz).date, tz)
            }
            val brewedTeas =
                data.sessions.map { it.session.teaId }.distinct().mapNotNull(teasMap::get)
            _state.update {
                it.copy(
                    allSessions = data.sessions,
                    groups = groups,
                    teas = teasMap,
                    brewedTeas = brewedTeas,
                    teaTypes = data.types.associateBy { type -> type.id },
                    vessels = vesselsMap,
                    tagVocabulary = data.vocabulary,
                    isLoading = false,
                    isEmpty = filteredSessions.isEmpty(),
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .catch { e ->
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_sessions)
            }
            .launchIn(viewModelScope)
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
    filters = HistoryFilters(
        teaTypeIds = setOfNotNull(route.filterTeaTypeId),
        teaId = route.filterTeaId,
        dateRangeStart = route.filterDateStart?.let(LocalDate::parse),
        dateRangeEnd = route.filterDateEnd?.let(LocalDate::parse),
    ),
)

internal fun List<SessionWithSteeps>.filteredBy(
    filters: HistoryFilters,
    teas: Map<String, Tea>,
): List<SessionWithSteeps> {
    var filtered = this

    if (filters.teaTypeIds.isNotEmpty()) {
        filtered = filtered.filter { session ->
            teas[session.session.teaId]?.teaTypeId in filters.teaTypeIds
        }
    }

    if (filters.teaId != null) {
        filtered = filtered.filter { it.session.teaId == filters.teaId }
    }

    if (filters.dateRangeStart != null || filters.dateRangeEnd != null) {
        filtered = filtered.filter { session ->
            val sessionDate =
                session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date

            val afterStart = filters.dateRangeStart?.let { sessionDate >= it } ?: true
            val beforeEnd = filters.dateRangeEnd?.let { sessionDate <= it } ?: true

            afterStart && beforeEnd
        }
    }

    if (filters.minRating != null) {
        filtered = filtered.filter { session ->
            session.effectiveRating?.let { it >= filters.minRating } ?: false
        }
    }

    if (filters.vesselIds.isNotEmpty()) {
        filtered = filtered.filter { it.session.vesselId in filters.vesselIds }
    }

    return filtered
}

/**
 * What History's search reads. Carried-over tags count: search looks for the word, not a
 * judgment. [tagLabels] are the names shown, by tag id.
 */
internal fun SessionWithSteeps.searchFields(
    teas: Map<String, Tea>,
    vessels: Map<String, BrewingVessel>,
    tagLabels: Map<String, String>,
): List<String?> {
    val tea = teas[session.teaId]
    val tagIds = steeps.flatMap { steep -> steep.tags.map { it.tagId } }.distinct()
    return listOf(
        tea?.name,
        tea?.producer,
        session.notes,
        session.location,
        vessels[session.vesselId]?.name
    ) +
            steeps.map { it.notes } +
            tagIds.map { tagLabels[it] }
}

private val SEARCH_DEBOUNCE = 300.milliseconds

sealed interface HistoryNavEvent {
    data class NavigateToSession(val sessionId: String) : HistoryNavEvent
    data class NavigateToTimer(val sessionId: String) : HistoryNavEvent
}

private data class HistoryData(
    val sessions: List<SessionWithSteeps>,
    val teas: List<Tea>,
    val types: List<TeaType>,
    val vessels: List<BrewingVessel>,
    val vocabulary: List<TastingTag> = emptyList(),
    val tagLabels: Map<String, String> = emptyMap(),
)
