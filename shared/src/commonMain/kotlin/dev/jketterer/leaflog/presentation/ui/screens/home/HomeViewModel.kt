package dev.jketterer.leaflog.presentation.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.models.InProgressSessionDetails
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.dominantTags
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.FinishSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetDailyStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.domain.usecases.session.ReopenSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import dev.jketterer.leaflog.domain.usecases.timer.RestoreTimerStateUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessage
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.messages.UiText
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.viewmodel.createBrewAgainDelegate
import dev.jketterer.leaflog.presentation.ui.viewmodel.createInProgressSessionDelegate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_delete_session
import leaf_log.shared.generated.resources.couldnt_load
import leaf_log.shared.generated.resources.couldnt_start_session
import leaf_log.shared.generated.resources.couldnt_undo
import leaf_log.shared.generated.resources.session_finished
import leaf_log.shared.generated.resources.undo
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Pending actions that can be deferred when an in-progress session conflict is detected.
 */
private sealed interface HomePendingAction {
    data object NavigateToLogTea : HomePendingAction
    data object ShowDurationSheet : HomePendingAction
    data class BrewAgain(val sessionId: String) : HomePendingAction
    data class AdjustBrew(val sessionId: String) : HomePendingAction
}

class HomeViewModel(
    private val route: NavRoute.HomeRoute,
    private val savedStateHandle: SavedStateHandle,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val vesselRepository: BrewingVesselRepository,
    private val tastingTagRepository: TastingTagRepository,
    private val getDailyStatsUseCase: GetDailyStatsUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val reopenSessionUseCase: ReopenSessionUseCase,
    private val discardSteepUseCase: DiscardSteepUseCase,
    private val getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
    private val restoreTimerStateUseCase: RestoreTimerStateUseCase,
    private val timerService: TimerService,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState(today = currentDate()))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _navEvents = Channel<HomeNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<HomeIntent>()

    private var loadJob: Job? = null

    private val inProgressDelegate = createInProgressSessionDelegate<HomeState, HomePendingAction>(
        getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
        finishSessionUseCase = finishSessionUseCase,
        discardSteepUseCase = discardSteepUseCase,
        timerService = timerService,
        stateFlow = _state,
        getDialogState = { it.inProgressDialogState },
        setDialogState = { state, dialogState -> state.copy(inProgressDialogState = dialogState) },
        onFailure = messages::failure,
        onResume = { session -> handleResumeSession(session) },
        executePendingAction = { action ->
            when (action) {
                is HomePendingAction.NavigateToLogTea ->
                    _navEvents.trySend(HomeNavEvent.NavigateToLogTea(NavRoute.LogTeaRoute()))

                is HomePendingAction.ShowDurationSheet ->
                    _state.update { it.copy(showDurationSheet = true) }

                is HomePendingAction.BrewAgain ->
                    brewAgain(action.sessionId)

                is HomePendingAction.AdjustBrew ->
                    _navEvents.trySend(
                        HomeNavEvent.NavigateToLogTea(NavRoute.LogTeaRoute(sourceSessionId = action.sessionId)),
                    )
            }
        },
    )

    private val brewAgainDelegate = createBrewAgainDelegate(
        teaSessionRepository = teaSessionRepository,
        teaRepository = teaRepository,
        teaTypeRepository = teaTypeRepository,
        brewingVesselRepository = vesselRepository,
        tastingTagRepository = tastingTagRepository,
        stateFlow = _state,
        getSheetState = { it.brewAgainSheet },
        setSheetState = { state, sheet -> state.copy(brewAgainSheet = sheet) },
        onFailure = messages::failure,
        onStart = { inProgressDelegate.checkAndProceed(HomePendingAction.BrewAgain(it)) },
        onAdjust = { inProgressDelegate.checkAndProceed(HomePendingAction.AdjustBrew(it)) },
    )

    init {
        collectTimerState()
        onIntent(HomeIntent.LoadData)
        offerUndoFinish()
    }

    // Once per entry. The saved flag outlives process death, so a restored Home stays quiet
    private fun offerUndoFinish() {
        val sessionId = route.finishedSessionId ?: return
        if (savedStateHandle.get<Boolean>(UNDO_OFFERED_KEY) == true) return
        savedStateHandle[UNDO_OFFERED_KEY] = true
        messages.show(
            UiMessage(
                text = UiText.Plain(Res.string.session_finished),
                action = UiMessage.Action(
                    label = Res.string.undo,
                    intent = HomeIntent.UndoFinishClicked(sessionId),
                ),
            ),
        )
    }

    private fun collectTimerState() {
        viewModelScope.launch {
            timerService.timerState.collect { timerState ->
                // Only update if there's an active timer (has a session ID)
                val liveState = if (timerState.sessionId != null) timerState else null
                _state.update { it.copy(liveTimerState = liveState) }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadData -> loadData()
            is HomeIntent.Refresh -> refresh()

            // The app can sit in the background past midnight, so today's stats move to the new day
            is HomeIntent.Resumed ->
                _state.update { it.copy(today = currentDate(), greeting = generateGreeting()) }

            is HomeIntent.LogTeaClicked -> {
                _state.update { it.copy(isFabExpanded = false) }
                inProgressDelegate.checkAndProceed(HomePendingAction.NavigateToLogTea)
            }

            is HomeIntent.QuickTimerClicked -> {
                _state.update { it.copy(isFabExpanded = false) }
                inProgressDelegate.checkAndProceed(HomePendingAction.ShowDurationSheet)
            }

            is HomeIntent.StartQuickTimer -> {
                _state.update { it.copy(showDurationSheet = false) }
                // Running already: the tea is poured by the time a timer is remembered. The Timer
                // takes it from here
                val duration = intent.durationSeconds.seconds
                timerService.updateState(
                    TimerState(
                        totalDuration = duration,
                        remainingDuration = duration,
                        status = TimerStatus.RUNNING,
                        startedAt = clock.now(),
                    ),
                )
                _navEvents.trySend(HomeNavEvent.NavigateToQuickTimer)
            }

            is HomeIntent.DismissDurationSheet -> {
                _state.update { it.copy(showDurationSheet = false) }
            }

            is HomeIntent.FabExpandedChanged -> {
                _state.update { it.copy(isFabExpanded = intent.expanded) }
            }

            is HomeIntent.UndoFinishClicked -> undoFinish(intent.sessionId)

            is HomeIntent.BrewAgainCardClicked -> brewAgainDelegate.openPair(intent.teaId, intent.vesselId)
            is HomeIntent.BrewAgain -> brewAgainDelegate.onIntent(intent.intent)

            is HomeIntent.DeleteSessionClicked -> _state.update { it.copy(sessionPendingDelete = intent.sessionId) }
            is HomeIntent.ConfirmDeleteSession -> confirmDeleteSession()
            is HomeIntent.CancelDeleteSession -> _state.update { it.copy(sessionPendingDelete = null) }

            is HomeIntent.SessionClicked -> handleSessionClick(intent.sessionId)
            is HomeIntent.ViewAllSessionsClicked -> _navEvents.trySend(HomeNavEvent.NavigateToHistory())
            is HomeIntent.ViewAllStatsClicked -> _navEvents.trySend(HomeNavEvent.NavigateToAnalytics)
            is HomeIntent.SettingsClicked -> _navEvents.trySend(HomeNavEvent.NavigateToSettings)

            is HomeIntent.DailyStatsTodaySessionsClicked -> {
                val today = clock.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                _navEvents.trySend(
                    HomeNavEvent.NavigateToHistory(
                        filterDateStart = today,
                        filterDateEnd = today
                    )
                )
            }


            is HomeIntent.DailyStatsTeasCardClicked -> _navEvents.trySend(HomeNavEvent.NavigateToCollection)

            is HomeIntent.ResumeInProgressClicked -> handleResumeInProgress()

            // In-progress session conflict dialog
            is HomeIntent.ResumeInProgressFromDialog -> inProgressDelegate.resume()
            is HomeIntent.DismissInProgressDialog -> inProgressDelegate.dismissDialog()
            is HomeIntent.CompleteInProgressAndContinue -> inProgressDelegate.completeAndContinue()
            is HomeIntent.DiscardInProgressAndContinue -> inProgressDelegate.discardAndContinue()
        }
    }

    private fun handleResumeInProgress() {
        val inProgress = _state.value.mostRecentInProgress?.session ?: return
        handleResumeSession(inProgress)
    }

    private fun handleResumeSession(session: SessionWithSteeps) {
        val sessionId = session.session.id
        when (session.currentSteep.resolvedTimerStatus(clock.now())) {
            TimerStatus.COMPLETE -> _navEvents.trySend(HomeNavEvent.CompleteSession(sessionId))
            else -> _navEvents.trySend(HomeNavEvent.NavigateToTimer(sessionId))
        }
    }

    private fun undoFinish(sessionId: String) {
        viewModelScope.launch {
            reopenSessionUseCase(sessionId)
                .onSuccess { _navEvents.send(HomeNavEvent.CompleteSession(sessionId)) }
                .onFailure { messages.failure(it, Res.string.couldnt_undo) }
        }
    }

    private fun handleSessionClick(sessionId: String) {
        val session =
            _state.value.recentSessionsWithTea.find { it.session.session.id == sessionId }?.session
        if (session?.session?.status == SessionStatus.IN_PROGRESS) {
            handleResumeSession(session)
        } else {
            _navEvents.trySend(HomeNavEvent.NavigateToSession(sessionId))
        }
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val greeting = generateGreeting()
                _state.update { it.copy(greeting = greeting) }

                val statsLoaded = MutableStateFlow(false)
                val sessionsLoaded = MutableStateFlow(false)
                val inProgressCountLoaded = MutableStateFlow(false)
                val inProgressRecentLoaded = MutableStateFlow(false)
                val brewAgainLoaded = MutableStateFlow(false)

                launch { collectDailyStats(statsLoaded) }
                launch { collectRecentSessions(sessionsLoaded) }
                launch { collectInProgressCount(inProgressCountLoaded) }
                launch { collectMostRecentInProgress(inProgressRecentLoaded) }
                launch { collectBrewAgainCards(brewAgainLoaded) }

                // Clear loading state once all collectors have emitted at least once
                launch {
                    combine(
                        statsLoaded,
                        sessionsLoaded,
                        inProgressCountLoaded,
                        inProgressRecentLoaded,
                        brewAgainLoaded,
                    ) { loaded -> loaded.all { it } }
                        .first { it }
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun collectDailyStats(loaded: MutableStateFlow<Boolean>) {
        _state.mapNotNull { it.today }
            .distinctUntilChanged()
            .flatMapLatest { getDailyStatsUseCase(it) }
            .catch { e ->
                messages.failure(e, Res.string.couldnt_load)
                loaded.value = true
            }
            .collect { stats ->
                _state.update { it.copy(dailyStats = stats) }
                loaded.value = true
            }
    }

    private suspend fun collectRecentSessions(loaded: MutableStateFlow<Boolean>) {
        combine(
            teaSessionRepository.getRecentSessionsFlow(limit = 5),
            teaRepository.getAllFlow(),
            tastingTagRepository.getAllFlow(),
            ::Triple,
        )
            .catch { e ->
                messages.failure(e, Res.string.couldnt_load)
                loaded.value = true
            }
            .collect { (sessions, teas, vocabulary) ->
                val teaMap = teas.associateBy { it.id }

                _state.update {
                    it.copy(
                        recentSessionsWithTea = sessions.map { session ->
                            val tea = teaMap[session.session.teaId]
                            SessionWithTeaData(
                                session = session,
                                teaName = tea?.name ?: "Unknown Tea",
                                tags = session.dominantTags(vocabulary),
                            )
                        },
                        isEmpty = sessions.isEmpty(),
                    )
                }
                loaded.value = true
            }
    }

    private suspend fun collectInProgressCount(loaded: MutableStateFlow<Boolean>) {
        teaSessionRepository.getInProgressSessionsFlow()
            .map { it.size }
            .catch { e ->
                Logger.w(tag = "Home") { "Failed to load in-progress count: ${e.message}" }
                loaded.value = true
            }
            .collect { count ->
                _state.update { it.copy(inProgressSessionsCount = count) }
                loaded.value = true
            }
    }

    private suspend fun collectMostRecentInProgress(loaded: MutableStateFlow<Boolean>) {
        combine(
            teaSessionRepository.getInProgressSessionsFlow(),
            teaRepository.getAllFlow(),
            vesselRepository.getAllFlow(),
        ) { inProgressSessions, teas, vessels ->
            val mostRecent = inProgressSessions.firstOrNull() ?: return@combine null
            val tea = teas.find { it.id == mostRecent.session.teaId }
            val vessel = vessels.find { it.id == mostRecent.session.vesselId }
            InProgressSessionDetails(
                session = mostRecent,
                teaName = tea?.name ?: "Unknown Tea",
                vesselName = vessel?.name ?: "Unknown Vessel",
            )
        }
            .catch { e ->
                Logger.w(tag = "Home") { "Failed to load most recent in-progress session: ${e.message}" }
                loaded.value = true
            }
            .collect { inProgressInfo ->
                val resolvedStatus = inProgressInfo?.session?.currentSteep
                    ?.resolvedTimerStatus(clock.now())
                _state.update {
                    it.copy(
                        mostRecentInProgress = inProgressInfo,
                        inProgressTimerStatus = resolvedStatus,
                        resolvedTimerProgress = inProgressInfo?.let { info -> restoreTimerStateUseCase(info.session) }
                            ?.progress,
                    )
                }
                loaded.value = true
            }
    }

    private suspend fun collectBrewAgainCards(loaded: MutableStateFlow<Boolean>) {
        combine(
            // reads every session for the pairs' lasts; a recent-pairs query if History ever pages
            teaSessionRepository.getAllSessionsFlow(),
            teaRepository.getAllFlow(),
            teaTypeRepository.getAllFlow(),
            vesselRepository.getAllFlow(),
            ::brewAgainCards,
        )
            .catch { e ->
                Logger.w(tag = "Home") { "Failed to load brew again cards: ${e.message}" }
                loaded.value = true
            }
            .collect { cards ->
                _state.update { it.copy(brewAgainCards = cards) }
                loaded.value = true
            }
    }

    private fun refresh() {
        loadData()
    }

    private fun currentDate(): LocalDate =
        clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun generateGreeting(): String {
        val now = clock.now()
        val hour = now.toLocalDateTime(TimeZone.currentSystemDefault()).hour

        return when (hour) {
            in 0..4 -> "Good night"
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    private fun brewAgain(sessionId: String) = viewModelScope.launch {
        val source = teaSessionRepository.getSession(sessionId) ?: return@launch
        startSessionUseCase(source)
            .onSuccess { _navEvents.send(HomeNavEvent.NavigateToTimer(it.session.id)) }
            .onFailure { messages.failure(it, Res.string.couldnt_start_session) }
    }

    private fun confirmDeleteSession() = viewModelScope.launch {
        val sessionId = _state.value.sessionPendingDelete ?: return@launch
        _state.update { it.copy(sessionPendingDelete = null) }
        deleteSessionUseCase(sessionId)
            .onSuccess { timerService.onSessionResolved(sessionId) }
            .onFailure { messages.failure(it, Res.string.couldnt_delete_session) }
    }
}

data class SessionWithTeaData(
    val session: SessionWithSteeps,
    val teaName: String = "Unknown Tea",
    val tags: List<TastingTag> = emptyList(),
)

sealed interface HomeNavEvent {
    data class NavigateToHistory(
        val filterDateStart: String? = null,
        val filterDateEnd: String? = null,
    ) : HomeNavEvent

    data object NavigateToCollection : HomeNavEvent
    data class NavigateToSession(val sessionId: String) : HomeNavEvent
    data class NavigateToLogTea(val route: NavRoute.LogTeaRoute) : HomeNavEvent

    data object NavigateToSettings : HomeNavEvent
    data class CompleteSession(val sessionId: String) : HomeNavEvent
    data class NavigateToTimer(val sessionId: String) : HomeNavEvent
    data object NavigateToQuickTimer : HomeNavEvent
    data object NavigateToAnalytics : HomeNavEvent
}

private const val UNDO_OFFERED_KEY = "undoOffered"
