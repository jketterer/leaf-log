package dev.jketterer.leaflog.presentation.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.InProgressSessionDetails
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.configuration.PinBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.BrewAgainUseCase
import dev.jketterer.leaflog.domain.usecases.session.CompleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.DeleteSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetDailyStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetInProgressSessionInfoUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.createInProgressSessionDelegate
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

/**
 * Pending actions that can be deferred when an in-progress session conflict is detected.
 */
private sealed interface HomePendingAction {
    data object NavigateToLogTea : HomePendingAction
    data object ShowDurationSheet : HomePendingAction
    data class BrewAgain(val session: TeaSession) : HomePendingAction
    data class QuickBrew(
        val teaId: String,
        val vesselId: String,
        val configurationId: String,
    ) : HomePendingAction
}

class HomeViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val vesselRepository: BrewingVesselRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getDailyStatsUseCase: GetDailyStatsUseCase,
    private val brewAgainUseCase: BrewAgainUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val getInProgressSessionInfoUseCase: GetInProgressSessionInfoUseCase,
    private val pinBrewingConfigurationUseCase: PinBrewingConfigurationUseCase,
    private val timerService: TimerService,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _navEvents = Channel<HomeNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    private var loadJob: Job? = null

    private val inProgressDelegate = createInProgressSessionDelegate<HomeState, HomePendingAction>(
        getInProgressSessionInfoUseCase = getInProgressSessionInfoUseCase,
        completeSessionUseCase = completeSessionUseCase,
        deleteSessionUseCase = deleteSessionUseCase,
        stateFlow = _state,
        getDialogState = { it.inProgressDialogState },
        setDialogState = { state, dialogState -> state.copy(inProgressDialogState = dialogState) },
        setError = { state, error -> state.copy(error = error) },
        onResume = { session -> handleResumeSession(session) },
        executePendingAction = { action ->
            when (action) {
                is HomePendingAction.NavigateToLogTea ->
                    _navEvents.trySend(HomeNavEvent.NavigateToLogTea())

                is HomePendingAction.ShowDurationSheet ->
                    _state.update { it.copy(showDurationSheet = true) }

                is HomePendingAction.BrewAgain ->
                    brewAgain(action.session)

                is HomePendingAction.QuickBrew ->
                    _navEvents.trySend(
                        HomeNavEvent.NavigateToLogTea(
                            teaId = action.teaId,
                            vesselId = action.vesselId,
                            configurationId = action.configurationId,
                        )
                    )
            }
        },
    )

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
                inProgressDelegate.checkAndProceed(HomePendingAction.NavigateToLogTea)
            }

            is HomeIntent.QuickTimerClicked -> {
                _state.update { it.copy(isFabExpanded = false) }
                inProgressDelegate.checkAndProceed(HomePendingAction.ShowDurationSheet)
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
                inProgressDelegate.checkAndProceed(HomePendingAction.BrewAgain(session))
            }

            is HomeIntent.QuickBrewClicked -> {
                inProgressDelegate.checkAndProceed(
                    HomePendingAction.QuickBrew(
                        teaId = intent.teaId,
                        vesselId = intent.vesselId,
                        configurationId = intent.configurationId,
                    )
                )
            }

            is HomeIntent.EditQuickBrewClicked -> {
                _navEvents.trySend(
                    HomeNavEvent.NavigateToEditBrewingMethod(
                        intent.teaId,
                        intent.configurationId
                    )
                )
            }

            is HomeIntent.ManageQuickBrewClicked ->
                _state.update { it.copy(showManageQuickBrewSheet = true) }

            is HomeIntent.DismissManageQuickBrewSheet ->
                _state.update { it.copy(showManageQuickBrewSheet = false) }

            is HomeIntent.PinConfigurationToggled -> pinConfiguration(
                intent.configurationId,
                intent.isPinned
            )

            is HomeIntent.PinnedConfigurationsReordered -> reorderPinnedConfigurations(intent.orderedIds)

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

    private fun handleResumeSession(session: TeaSession) {
        when (session.timerStatus) {
            TimerStatus.COMPLETE -> _navEvents.trySend(HomeNavEvent.CompleteSession(session.id))
            else -> _navEvents.trySend(HomeNavEvent.NavigateToTimer(session.id))
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
                val targetSession = activeChildSteep ?: session
                handleResumeSession(targetSession)
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
                val configsLoaded = MutableStateFlow(false)

                launch { collectDailyStats(statsLoaded) }
                launch { collectRecentSessions(sessionsLoaded) }
                launch { collectInProgressCount(inProgressCountLoaded) }
                launch { collectMostRecentInProgress(inProgressRecentLoaded) }
                launch { collectQuickBrewConfigurations(configsLoaded) }

                // Clear loading state once all collectors have emitted at least once
                launch {
                    combine(
                        statsLoaded,
                        sessionsLoaded,
                        inProgressCountLoaded,
                        inProgressRecentLoaded,
                        configsLoaded,
                    ) { loaded -> loaded.all { it } }
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
                Logger.w("Home") { "Failed to load in-progress count: ${e.message}" }
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
            InProgressSessionDetails(
                session = mostRecent,
                teaName = tea?.name ?: "Unknown Tea",
                vesselName = vessel?.name ?: "Unknown Vessel",
            )
        }
            .catch { e ->
                Logger.w("Home") { "Failed to load most recent in-progress session: ${e.message}" }
                loaded.value = true
            }
            .collect { inProgressInfo ->
                _state.update { it.copy(mostRecentInProgress = inProgressInfo) }
                loaded.value = true
            }
    }

    private suspend fun collectQuickBrewConfigurations(loaded: MutableStateFlow<Boolean>) {
        combine(
            brewingConfigurationRepository.getAllActiveFlow(),
            teaRepository.getAllFlow(),
            teaTypeRepository.getAllFlow(),
            vesselRepository.getAllFlow(),
        ) { configs, teas, types, vessels ->
            val teaMap = teas.associateBy { it.id }
            val typeMap = types.associateBy { it.id }
            val vesselMap = vessels.associateBy { it.id }
            configs.mapNotNull { config ->
                val tea = teaMap[config.teaId] ?: return@mapNotNull null
                val type = typeMap[tea.teaTypeId]
                val vessel = vesselMap[config.vesselId] ?: return@mapNotNull null
                QuickBrewCardData(
                    configuration = config,
                    teaName = tea.name,
                    teaTypeName = type?.name ?: "Unknown",
                    teaTypeColorHex = type?.colorHex,
                    vesselName = vessel.name,
                )
            }
        }
            .catch { e ->
                Logger.w("Home") { "Failed to load quick brew configs: ${e.message}" }
                loaded.value = true
            }
            .collect { allConfigs ->
                _state.update {
                    it.copy(
                        allBrewingConfigurations = allConfigs,
                        quickBrewConfigurations = allConfigs.filter { data ->
                            data.configuration.isPinned || data.configuration.timesUsed > 0
                        },
                    )
                }
                loaded.value = true
            }
    }

    private fun pinConfiguration(configId: String, isPinned: Boolean) = viewModelScope.launch {
        pinBrewingConfigurationUseCase(configId, isPinned)
            .onFailure { e ->
                _state.update { it.copy(error = "Failed to update pin: ${e.message}") }
            }
    }

    private fun reorderPinnedConfigurations(orderedIds: List<String>) = viewModelScope.launch {
        runCatching { brewingConfigurationRepository.reorderPinnedConfigurations(orderedIds) }
            .onFailure { e ->
                _state.update { it.copy(error = "Failed to reorder: ${e.message}") }
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

data class QuickBrewCardData(
    val configuration: BrewingConfiguration,
    val teaName: String,
    val teaTypeName: String,
    val teaTypeColorHex: String?,
    val vesselName: String,
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
    data class NavigateToEditBrewingMethod(val teaId: String, val configurationId: String) :
        HomeNavEvent
}

private data class HomeSessionData(
    val sessions: List<TeaSession>,
    val teas: List<Tea>,
    val types: List<TeaType>,
    val vessels: List<BrewingVessel>,
)
