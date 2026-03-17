package dev.jketterer.leaflog.presentation.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.session.BrewAgainUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetDailyStatsUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class HomeViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val vesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getDailyStatsUseCase: GetDailyStatsUseCase,
    private val brewAgainUseCase: BrewAgainUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val timerService: TimerService,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _navEvents = Channel<HomeNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
        collectTimerState()
        onIntent(HomeIntent.LoadData)
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
            is HomeIntent.ClearError -> clearError()

            is HomeIntent.LogTeaClicked -> {
                _state.update { it.copy(isFabExpanded = false) }
                _navEvents.trySend(HomeNavEvent.NavigateToLogTea())
            }

            is HomeIntent.QuickTimerClicked -> {
                _state.update { it.copy(isFabExpanded = false, showDurationSheet = true) }
            }

            is HomeIntent.StartQuickTimer -> {
                _state.update { it.copy(showDurationSheet = false) }
                _navEvents.trySend(HomeNavEvent.NavigateToQuickTimer(intent.durationSeconds))
            }

            is HomeIntent.DismissDurationSheet -> {
                _state.update { it.copy(showDurationSheet = false) }
            }

            is HomeIntent.FabExpandedChanged -> {
                _state.update { it.copy(isFabExpanded = intent.expanded) }
            }

            is HomeIntent.BrewAgainClicked -> {
                val session = _state.value.recentSessionsWithTea
                    .find { it.session.id == intent.sessionId }?.session ?: return
                brewAgain(session)
            }

            is HomeIntent.DeleteSessionClicked -> _state.update { it.copy(sessionPendingDelete = intent.sessionId) }
            is HomeIntent.ConfirmDeleteSession -> confirmDeleteSession()
            is HomeIntent.CancelDeleteSession -> _state.update { it.copy(sessionPendingDelete = null) }

            is HomeIntent.SessionClicked -> handleSessionClick(intent.sessionId)
            is HomeIntent.ViewAllSessionsClicked -> _navEvents.trySend(HomeNavEvent.NavigateToHistory())
            is HomeIntent.ViewAllStatsClicked -> _navEvents.trySend(HomeNavEvent.NavigateToAnalytics)
            is HomeIntent.SettingsClicked -> _navEvents.trySend(HomeNavEvent.NavigateToSettings)

            is HomeIntent.DailyStatsTodaySessionsClicked -> {
                val today = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                _navEvents.trySend(
                    HomeNavEvent.NavigateToHistory(
                        filterDateStart = today,
                        filterDateEnd = today
                    )
                )
            }

            is HomeIntent.DailyStatsWaterCardClicked -> toggleVolumeUnit()

            is HomeIntent.DailyStatsTeasCardClicked -> _navEvents.trySend(HomeNavEvent.NavigateToCollection)

            is HomeIntent.ResumeInProgressClicked -> handleResumeInProgress()
        }
    }

    private fun handleResumeInProgress() {
        val inProgress = _state.value.mostRecentInProgress?.session ?: return
        when (inProgress.timerStatus) {
            TimerStatus.COMPLETE -> _navEvents.trySend(HomeNavEvent.CompleteSession(inProgress.id))
            else -> _navEvents.trySend(HomeNavEvent.NavigateToTimer(inProgress.id))
        }
    }

    private fun handleSessionClick(sessionId: String) {
        val session =
            _state.value.recentSessionsWithTea.find { it.session.id == sessionId }?.session
        if (session?.status == SessionStatus.IN_PROGRESS) {
            viewModelScope.launch {
                // Find the active child steep to navigate to, if any
                val activeChildSteep = teaSessionRepository.getChildSteeps(sessionId)
                    .find { it.status == SessionStatus.IN_PROGRESS }
                val targetSessionId = activeChildSteep?.id ?: sessionId
                _navEvents.trySend(HomeNavEvent.NavigateToTimer(targetSessionId))
            }
        } else {
            _navEvents.trySend(HomeNavEvent.NavigateToSession(sessionId))
        }
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val greeting = generateGreeting()
                _state.update { it.copy(greeting = greeting) }

                val statsLoaded = MutableStateFlow(false)
                val sessionsLoaded = MutableStateFlow(false)
                val inProgressCountLoaded = MutableStateFlow(false)
                val inProgressRecentLoaded = MutableStateFlow(false)

                launch { collectDailyStats(statsLoaded) }
                launch { collectRecentSessions(sessionsLoaded) }
                launch { collectInProgressCount(inProgressCountLoaded) }
                launch { collectMostRecentInProgress(inProgressRecentLoaded) }

                // Clear loading state once all collectors have emitted at least once
                launch {
                    combine(
                        statsLoaded,
                        sessionsLoaded,
                        inProgressCountLoaded,
                        inProgressRecentLoaded,
                    ) { s, r, c, m -> s && r && c && m }
                        .first { it }
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load data: ${e.message}",
                    )
                }
            }
        }
    }

    private suspend fun collectDailyStats(loaded: MutableStateFlow<Boolean>) {
        getDailyStatsUseCase()
            .catch { e ->
                _state.update {
                    it.copy(
                        error = "Failed to load statistics: ${e.message}",
                    )
                }
                loaded.value = true
            }
            .collect { stats ->
                _state.update { it.copy(dailyStats = stats) }
                loaded.value = true
            }
    }

    private suspend fun collectRecentSessions(loaded: MutableStateFlow<Boolean>) {
        combine(
            teaSessionRepository.getRecentFlow(limit = 5),
            teaRepository.getAllFlow(),
            teaTypeRepository.getAllFlow(),
            vesselRepository.getAllFlow(),
        ) { sessions, teas, types, vessels ->
            HomeSessionData(sessions, teas, types, vessels)
        }
            .catch { e ->
                _state.update {
                    it.copy(
                        error = "Failed to load sessions: ${e.message}",
                    )
                }
                loaded.value = true
            }
            .collect { (sessions, teas, types, vessels) ->
                // Create lookup maps for efficient access
                val teaMap = teas.associateBy { it.id }
                val typeMap = types.associateBy { it.id }
                val vesselMap = vessels.associateBy { it.id }

                _state.update {
                    it.copy(
                        recentSessionsWithTea = sessions.map { session ->
                            val tea = teaMap[session.teaId]
                            val type = tea?.let { t -> typeMap[t.teaTypeId] }
                            val vessel = vesselMap[session.vesselId]
                            SessionWithTeaData(
                                session = session,
                                teaName = tea?.name ?: "Unknown Tea",
                                teaTypeName = type?.name ?: "Unknown Type",
                                teaPhotoUrl = tea?.photos?.firstOrNull(),
                                vesselName = vessel?.name ?: "Unknown Vessel",
                            )
                        },
                        isEmpty = sessions.isEmpty(),
                    )
                }
                loaded.value = true
            }
    }

    private suspend fun collectInProgressCount(loaded: MutableStateFlow<Boolean>) {
        teaSessionRepository.getInProgressCountFlow()
            .catch { e ->
                println("Failed to load in-progress count: ${e.message}")
                loaded.value = true
            }
            .collect { count ->
                _state.update { it.copy(inProgressSessionsCount = count) }
                loaded.value = true
            }
    }

    private suspend fun collectMostRecentInProgress(loaded: MutableStateFlow<Boolean>) {
        combine(
            teaSessionRepository.getInProgressFlow(),
            teaRepository.getAllFlow(),
            vesselRepository.getAllFlow(),
        ) { inProgressSessions, teas, vessels ->
            val mostRecent = inProgressSessions.firstOrNull() ?: return@combine null
            val tea = teas.find { it.id == mostRecent.teaId }
            val vessel = vessels.find { it.id == mostRecent.vesselId }
            InProgressSessionInfo(
                session = mostRecent,
                teaName = tea?.name ?: "Unknown Tea",
                vesselName = vessel?.name ?: "Unknown Vessel",
            )
        }
            .catch { e ->
                println("Failed to load most recent in-progress session: ${e.message}")
                loaded.value = true
            }
            .collect { inProgressInfo ->
                _state.update { it.copy(mostRecentInProgress = inProgressInfo) }
                loaded.value = true
            }
    }

    private fun refresh() {
        loadData()
    }

    private fun generateGreeting(): String {
        val now = Clock.System.now()
        val hour = now.toLocalDateTime(TimeZone.currentSystemDefault()).hour

        return when (hour) {
            in 0..4 -> "Good night"
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    private fun brewAgain(session: TeaSession) = viewModelScope.launch {
        brewAgainUseCase(session)
            .onSuccess {
                _state.update { state -> state.copy(isFabExpanded = false) }
                _navEvents.send(HomeNavEvent.NavigateToTimer(it.id))
            }
            .onFailure { e ->
                _state.update { it.copy(error = "Failed to create new session: ${e.message}") }
            }
    }

    private fun confirmDeleteSession() = viewModelScope.launch {
        val sessionId = _state.value.sessionPendingDelete ?: return@launch
        _state.update { it.copy(sessionPendingDelete = null) }
        deleteSessionUseCase(sessionId)
            .onFailure { e ->
                _state.update { it.copy(error = "Failed to delete session: ${e.message}") }
            }
    }

    private fun toggleVolumeUnit() = viewModelScope.launch {
        val toggled = _state.value.userPreferences.volumeUnit.toggle()
        preferencesRepository.updateVolumeUnit(toggled)
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class SessionWithTeaData(
    val session: TeaSession,
    val teaName: String = "Unknown Tea",
    val teaTypeName: String = "Unknown Type",
    val teaPhotoUrl: String? = null,
    val vesselName: String = "Unknown Vessel",
)

sealed interface HomeNavEvent {
    data class NavigateToHistory(
        val filterDateStart: String? = null,
        val filterDateEnd: String? = null,
    ) : HomeNavEvent

    data object NavigateToCollection : HomeNavEvent
    data class NavigateToSession(val sessionId: String) : HomeNavEvent
    data class NavigateToLogTea(
        val teaId: String? = null,
        val vesselId: String? = null,
        val configurationId: String? = null,
    ) :
        HomeNavEvent

    data object NavigateToSettings : HomeNavEvent
    data class CompleteSession(val sessionId: String) : HomeNavEvent
    data class NavigateToTimer(val sessionId: String) : HomeNavEvent
    data class NavigateToQuickTimer(val durationSeconds: Int) : HomeNavEvent
    data object NavigateToAnalytics : HomeNavEvent
}

private data class HomeSessionData(
    val sessions: List<TeaSession>,
    val teas: List<Tea>,
    val types: List<TeaType>,
    val vessels: List<BrewingVessel>,
)
