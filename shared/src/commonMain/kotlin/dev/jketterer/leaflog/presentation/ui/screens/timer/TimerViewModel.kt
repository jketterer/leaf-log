package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.session.AttachTeaUseCase
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.ResolvePlanUseCase
import dev.jketterer.leaflog.domain.usecases.session.SaveTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.AdjustTimeUseCase
import dev.jketterer.leaflog.domain.usecases.timer.PauseTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.RestoreTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.ResumeTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.StartTimerUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.screens.steepcomplete.nextSteepTime
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_adjust_timer
import leaf_log.shared.generated.resources.couldnt_discard_steep
import leaf_log.shared.generated.resources.couldnt_load_session
import leaf_log.shared.generated.resources.couldnt_pause_timer
import leaf_log.shared.generated.resources.couldnt_resume_timer
import leaf_log.shared.generated.resources.couldnt_save_changes
import leaf_log.shared.generated.resources.couldnt_save_session
import leaf_log.shared.generated.resources.couldnt_save_tea
import leaf_log.shared.generated.resources.couldnt_start_timer
import leaf_log.shared.generated.resources.failure_not_found
import kotlin.time.Clock
import kotlin.time.Duration

class TimerViewModel(
    private val route: NavRoute.TimerRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: TimerNotificationService,
    private val timerService: TimerService,
    private val startTimerUseCase: StartTimerUseCase,
    private val pauseTimerUseCase: PauseTimerUseCase,
    private val resumeTimerUseCase: ResumeTimerUseCase,
    private val adjustTimeUseCase: AdjustTimeUseCase,
    private val saveTimerStateUseCase: SaveTimerStateUseCase,
    private val restoreTimerStateUseCase: RestoreTimerStateUseCase,
    private val editSteepUseCase: EditSteepUseCase,
    private val editSessionUseCase: EditSessionUseCase,
    private val discardSteepUseCase: DiscardSteepUseCase,
    private val resolvePlanUseCase: ResolvePlanUseCase,
    private val attachTeaUseCase: AttachTeaUseCase,
    private val createTeaUseCase: CreateTeaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TimerScreenState(isQuickTimer = route.sessionId == null, isLoading = true),
    )
    val state: StateFlow<TimerScreenState> = _state.asStateFlow()

    private val _navigationEvents = Channel<TimerNavEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
        collectTimerState()
        val sessionId = route.sessionId
        if (sessionId == null) initializeQuickTimer() else initialize(sessionId)
    }

    fun onIntent(intent: TimerIntent) {
        when (intent) {
            is TimerIntent.StartTimer -> startTimer()
            is TimerIntent.PauseTimer -> pauseTimer()
            is TimerIntent.ResumeTimer -> resumeTimer()
            is TimerIntent.AdjustTime -> adjustTime(intent.adjustment)
            is TimerIntent.ResetTimer -> showResetConfirmation()
            is TimerIntent.ConfirmReset -> confirmReset()
            is TimerIntent.CancelReset -> cancelReset()
            is TimerIntent.DiscardSession -> showDiscardSessionConfirmation()
            is TimerIntent.ConfirmStop -> confirmDiscardSession()
            is TimerIntent.CancelStop -> cancelDiscardSession()
            is TimerIntent.RestartTimer -> restartTimer()
            is TimerIntent.CompleteNow -> completeNow()
            is TimerIntent.DismissNotificationWarning -> _state.update { it.copy(showNotificationWarning = false) }
            is TimerIntent.BackClicked -> onBack()
            is TimerIntent.JustATimer -> endQuickTimer()
            is TimerIntent.ConfirmEndTimer -> endQuickTimer()
            is TimerIntent.CancelEndTimer -> _state.update { it.copy(showEndTimerConfirmation = false) }
            is TimerIntent.NextByFeel -> nextByFeel()
            is TimerIntent.ShowTeaPicker -> _state.update { it.copy(showTeaPicker = true) }
            is TimerIntent.HideTeaPicker -> _state.update { it.copy(showTeaPicker = false) }
            is TimerIntent.TeaPicked -> attachTea(intent.teaId)
            is TimerIntent.QuickAddTea -> quickAddTea(intent.name, intent.teaTypeId)

            is TimerIntent.EditSession -> openEditSheet()
            is TimerIntent.EditTemperatureChanged -> _state.update { it.copy(editTemperatureCelsius = intent.celsius) }
            is TimerIntent.EditWaterQuantityChanged -> _state.update { it.copy(editWaterQuantityMl = intent.milliliters) }
            is TimerIntent.EditTeaQuantityChanged -> _state.update { it.copy(editTeaQuantityGrams = intent.grams) }
            is TimerIntent.EditTeaBagModeChanged -> _state.update {
                it.copy(
                    editIsTeaBag = intent.isTeaBag,
                    editTeaQuantityGrams = if (intent.isTeaBag) null else it.editTeaQuantityGrams,
                )
            }

            is TimerIntent.EditWaterTypeChanged -> _state.update { it.copy(editWaterType = intent.waterType) }
            is TimerIntent.ConfirmEditSession -> confirmEditSession()
            is TimerIntent.CancelEditSession -> _state.update { it.copy(showEditSheet = false) }
        }
    }

    private fun collectTimerState() {
        viewModelScope.launch {
            timerService.timerState
                .collect { timerState ->
                    _state.update { it.copy(timerState = timerState) }

                    // Auto-complete when timer reaches zero.
                    // Guard on sessionId to prevent stale COMPLETE emissions from a previous
                    // session (that completed while no ViewModel was active) from triggering
                    // completion on the newly loaded session.
                    val currentSession = _state.value.session
                    if (timerState.status == TimerStatus.COMPLETE &&
                        currentSession != null &&
                        timerState.sessionId == currentSession.session.id &&
                        timerState.steepNumber == currentSession.currentSteep.number
                    ) {
                        handleTimerComplete()
                    }
                }
        }
    }

    // Takes a steep's id too: steep 1's id is the session's, and older notifications carry a steep's
    private fun initialize(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val session = teaSessionRepository.getSessionOfSteep(id)
                if (session == null) {
                    _state.update { it.copy(isLoading = false) }
                    messages.show(Res.string.failure_not_found)
                    return@launch
                }

                val sessionId = session.session.id
                val steep = session.currentSteep
                val tea = teaRepository.getById(session.session.teaId)
                val vessel = brewingVesselRepository.getById(session.session.vesselId)
                val hasNotificationPermission = notificationService.hasNotificationPermission()
                val nextSteep = nextSteepTime(session, resolvePlanUseCase(session))

                // Prefer live TimerService state if it's already tracking this steep
                // (e.g., navigating back while timer is still running)
                val currentTimerState = timerService.getCurrentState()
                val initialTimerState = if (currentTimerState.sessionId == sessionId &&
                    currentTimerState.steepNumber == steep.number &&
                    currentTimerState.status != TimerStatus.NOT_STARTED
                ) {
                    currentTimerState.copy(teaName = tea?.name ?: "")
                } else {
                    // Fall back to restoring from database (e.g., app was killed)
                    restoreTimerStateUseCase(session)?.copy(teaName = tea?.name ?: "")
                        ?: TimerState(
                            sessionId = sessionId,
                            teaId = session.session.teaId,
                            teaName = tea?.name ?: "",
                            steepNumber = steep.number,
                            totalDuration = steep.brewingTime,
                            remainingDuration = steep.brewingTime,
                            brewingTemperatureCelsius = steep.temperatureCelsius,
                        )
                }

                // The live TimerService state above can be stale (its countdown loop stops
                // advancing while the app is backgrounded), so re-derive against the clock
                // before deciding whether the timer has finished.
                val resolvedTimerState = initialTimerState.resolvedAt(Clock.System.now())
                    .copy(nextSteepDuration = nextSteep)

                // If timer was already complete (e.g., app killed during SteepComplete),
                // redirect to SteepComplete immediately
                if (resolvedTimerState.status == TimerStatus.COMPLETE) {
                    _navigationEvents.send(TimerNavEvent.NavigateToSteepComplete(sessionId))
                    return@launch
                }

                // Update timer service with restored/initial state
                timerService.updateState(resolvedTimerState)

                // If timer was running, restart the countdown
                if (resolvedTimerState.status == TimerStatus.RUNNING) {
                    timerService.startCountdown()
                }

                _state.update {
                    it.copy(
                        session = session,
                        tea = tea,
                        vessel = vessel,
                        timerState = resolvedTimerState,
                        showNotificationWarning = !hasNotificationPermission,
                        isLoading = false,
                    )
                }

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_session)
            }
        }
    }

    // Live when Home has just started it or a notification brought the user back; stored when
    // the process has died since
    private fun initializeQuickTimer() {
        viewModelScope.launch {
            combine(
                teaRepository.getAllFlow(),
                teaRepository.getRecentlyBrewedFlow(5),
                teaTypeRepository.getAllFlow(),
            ) { teas, recent, types -> Triple(teas, recent, types) }
                .collect { (teas, recent, types) ->
                    _state.update { it.copy(pickerTeas = teas, recentTeas = recent, teaTypes = types) }
                }
        }
        viewModelScope.launch {
            val live = timerService.getCurrentState()
                .takeIf { it.sessionId == null && it.totalDuration > Duration.ZERO }
            val timer = (live ?: preferencesRepository.getQuickTimer())
                ?.resolvedAt(Clock.System.now())
            if (timer == null) {
                // A notification left over from a timer since ended
                _navigationEvents.send(TimerNavEvent.NavigateBack)
                return@launch
            }

            timerService.updateState(timer)
            if (timer.isRunning) timerService.startCountdown()
            persist(timer)

            _state.update {
                it.copy(
                    timerState = timer,
                    showNotificationWarning = !notificationService.hasNotificationPermission(),
                    isLoading = false,
                )
            }
        }
    }

    /** A session's timer is kept on its steep, the Quick Timer in preferences. */
    private suspend fun persist(timer: TimerState) {
        if (timer.sessionId != null) {
            saveTimerStateUseCase(timer)
        } else {
            preferencesRepository.updateQuickTimer(timer)
        }
    }

    private fun startTimer() {
        val session = _state.value.session
        if (session == null && !_state.value.isQuickTimer) return
        val timer = _state.value.timerState

        viewModelScope.launch {
            val started = if (session != null) {
                startTimerUseCase(session = session, duration = timer.totalDuration)
            } else {
                Result.success(
                    timer.copy(
                        status = TimerStatus.RUNNING,
                        remainingDuration = timer.totalDuration,
                        startedAt = Clock.System.now(),
                        pausedAt = null,
                    ),
                )
            }
            started
                .onSuccess { timerState ->
                    val updatedState = timerState.copy(
                        teaName = _state.value.tea?.name ?: "",
                        nextSteepDuration = _state.value.timerState.nextSteepDuration,
                    )

                    timerService.updateState(updatedState)
                    timerService.startCountdown()

                    // Persist running state for restoration
                    persist(updatedState)
                }
                .onFailure { messages.failure(it, Res.string.couldnt_start_timer) }
        }
    }

    private fun pauseTimer() {
        viewModelScope.launch {
            // 1. Call use case - calculates paused state
            pauseTimerUseCase(timerService.getCurrentState())
                .onSuccess { newState ->
                    // 2. Update TimerService with new state
                    timerService.updateState(newState)

                    // 3. Cancel countdown
                    timerService.cancelCountdown()

                    // 4. Persist timer state for later restoration
                    persist(newState)
                }
                .onFailure { messages.failure(it, Res.string.couldnt_pause_timer) }
        }
    }

    private fun resumeTimer() {
        viewModelScope.launch {
            // 1. Call use case - recalculates state for resume
            resumeTimerUseCase(timerService.getCurrentState())
                .onSuccess { newState ->
                    // 2. Update TimerService with new state
                    timerService.updateState(newState)

                    // 3. Start countdown
                    timerService.startCountdown()

                    // 4. Persist, so a restore doesn't find the timer still paused
                    persist(newState)
                }
                .onFailure { messages.failure(it, Res.string.couldnt_resume_timer) }
        }
    }

    private fun adjustTime(adjustment: Duration) {
        viewModelScope.launch {
            // 1. Call use case - validates and calculates adjusted state
            adjustTimeUseCase(timerService.getCurrentState(), adjustment)
                .onSuccess { newState ->
                    // 2. Update TimerService with adjusted state
                    timerService.updateState(newState)

                    // 3. Reschedule completion alarm for new duration
                    timerService.onTimeAdjusted(newState)

                    // 4. Persist adjusted state so it survives process death
                    persist(newState)
                }
                .onFailure { messages.failure(it, Res.string.couldnt_adjust_timer) }
        }
    }

    private fun showResetConfirmation() {
        _state.update { it.copy(showResetConfirmation = true) }
    }

    private fun cancelReset() {
        _state.update { it.copy(showResetConfirmation = false) }
    }

    private fun confirmReset() {
        _state.update { it.copy(showResetConfirmation = false) }

        // Reset is simple state change - no use case needed. A Quick Timer has no planned time,
        // so it goes back to its length
        val current = timerService.getCurrentState()
        val brewingTime = _state.value.session?.currentSteep?.brewingTime ?: current.totalDuration
        val resetState = current.copy(
            totalDuration = brewingTime,
            remainingDuration = brewingTime,
            status = TimerStatus.NOT_STARTED,
            startedAt = null,
            pausedAt = null,
        )
        timerService.updateState(resetState)

        // Persist so leaving the screen or losing the process doesn't restore the paused timer
        viewModelScope.launch { persist(resetState) }
    }

    private fun showDiscardSessionConfirmation() {
        _state.update { it.copy(showDiscardSessionConfirmation = true) }
    }

    private fun cancelDiscardSession() {
        _state.update { it.copy(showDiscardSessionConfirmation = false) }
    }

    private fun confirmDiscardSession() {
        val session = _state.value.session ?: return
        _state.update { it.copy(showDiscardSessionConfirmation = false) }

        viewModelScope.launch {
            discardSteepUseCase(session.session.id)
                .onSuccess {
                    timerService.onSessionResolved(session.session.id)
                    _navigationEvents.send(TimerNavEvent.NavigateBack)
                }
                .onFailure { messages.failure(it, Res.string.couldnt_discard_steep) }
        }
    }

    private fun handleTimerComplete() {
        val session = _state.value.session ?: return

        viewModelScope.launch {
            val completedState = timerService.getCurrentState().completedAt(Clock.System.now())
            timerService.updateState(completedState)
            // Persist completed state for restoration
            saveTimerStateUseCase(completedState)
            // Reset singleton state so the next Timer screen (e.g., next steep) starts
            // clean and doesn't receive a stale COMPLETE emission in collectTimerState()
            timerService.stop()
            _navigationEvents.send(TimerNavEvent.NavigateToSteepComplete(session.session.id))
        }
    }

    private fun openEditSheet() {
        val session = _state.value.session ?: return
        val steep = session.currentSteep
        _state.update {
            it.copy(
                showEditSheet = true,
                editTemperatureCelsius = steep.temperatureCelsius,
                editWaterQuantityMl = steep.waterQuantityMl,
                editTeaQuantityGrams = session.session.teaQuantityGrams?.toDouble(),
                editIsTeaBag = session.session.teaQuantityGrams == null,
                editWaterType = session.session.waterType,
            )
        }
    }

    private fun confirmEditSession() {
        val session = _state.value.session ?: return
        val steep = session.currentSteep
        val currentState = _state.value
        val temperatureCelsius = currentState.editTemperatureCelsius ?: steep.temperatureCelsius
        val waterQuantityMl = currentState.editWaterQuantityMl ?: steep.waterQuantityMl
        val isTeaBag = currentState.editIsTeaBag
        val teaQuantityGrams = currentState.editTeaQuantityGrams?.toFloat()
        val waterType = currentState.editWaterType

        _state.update { it.copy(showEditSheet = false) }

        viewModelScope.launch {
            if (temperatureCelsius != steep.temperatureCelsius || waterQuantityMl != steep.waterQuantityMl) {
                editSteepUseCase(steep.id) {
                    it.copy(
                        temperatureCelsius = temperatureCelsius,
                        waterQuantityMl = waterQuantityMl,
                    )
                }.onFailure { e ->
                    messages.failure(e, Res.string.couldnt_save_changes)
                    return@launch
                }
            }
            editSessionUseCase(session.session.id) {
                it.copy(
                    teaQuantityGrams = if (isTeaBag) null else teaQuantityGrams ?: it.teaQuantityGrams,
                    waterType = waterType ?: it.waterType,
                )
            }
                .onSuccess { updated -> _state.update { it.copy(session = updated) } }
                .onFailure { messages.failure(it, Res.string.couldnt_save_changes) }
        }
    }

    // Already brewed before Start, Poured early after. The collector above sees COMPLETE and
    // finishes the steep, so it isn't finished twice. A Quick Timer has no steep, so it just stops
    private fun completeNow() {
        val isQuickTimer = _state.value.isQuickTimer
        if (_state.value.session == null && !isQuickTimer) return
        val completed = timerService.getCurrentState().completedAt(Clock.System.now())
        timerService.updateState(completed)
        if (isQuickTimer) {
            notificationService.onTimerStopped()
            viewModelScope.launch { persist(completed) }
        }
    }

    // The Quick Timer lives on its screen alone, since Home has nowhere to show it
    private fun onBack() {
        val state = _state.value
        when {
            !state.isQuickTimer -> _navigationEvents.trySend(TimerNavEvent.NavigateBack)
            state.timerState.isRunning || state.timerState.canResume ->
                _state.update { it.copy(showEndTimerConfirmation = true) }
            else -> endQuickTimer()
        }
    }

    private fun endQuickTimer() {
        _state.update { it.copy(showEndTimerConfirmation = false) }
        timerService.stop()
        notificationService.cancelTimerComplete()
        viewModelScope.launch {
            preferencesRepository.updateQuickTimer(null)
            _navigationEvents.send(TimerNavEvent.NavigateBack)
        }
    }

    // The steep this timer finished moves into its record, and the next waits for Start
    private fun nextByFeel() {
        val next = timerService.getCurrentState().nextByFeel()
        timerService.onNextSteep()
        timerService.updateState(next)
        viewModelScope.launch { persist(next) }
    }

    private fun quickAddTea(name: String, teaTypeId: String) {
        _state.update { it.copy(showTeaPicker = false) }
        viewModelScope.launch {
            createTeaUseCase(name = name, teaTypeId = teaTypeId)
                .onSuccess { attachTea(it.id) }
                .onFailure { messages.failure(it, Res.string.couldnt_save_tea) }
        }
    }

    private fun attachTea(teaId: String) {
        _state.update { it.copy(showTeaPicker = false) }
        viewModelScope.launch {
            val timer = timerService.getCurrentState().resolvedAt(Clock.System.now())
            attachTeaUseCase(teaId, timer, _state.value.userPreferences.defaultWaterType)
                .onSuccess { handOver(it, timer) }
                .onFailure { messages.failure(it, Res.string.couldnt_save_session) }
        }
    }

    // A finished steep goes to its review; one still brewing carries on in the session's Timer,
    // which picks the live timer up by its session
    private suspend fun handOver(session: SessionWithSteeps, timer: TimerState) {
        preferencesRepository.updateQuickTimer(null)
        val sessionId = session.session.id
        if (timer.isComplete) {
            timerService.onNextSteep()
            _navigationEvents.send(TimerNavEvent.NavigateToSteepComplete(sessionId))
        } else {
            timerService.updateState(
                timer.copy(
                    sessionId = sessionId,
                    teaId = session.session.teaId,
                    brewingTemperatureCelsius = session.currentSteep.temperatureCelsius,
                    finishedSteeps = emptyList(),
                ),
            )
            _navigationEvents.send(TimerNavEvent.NavigateToSessionTimer(sessionId))
        }
    }

    private fun restartTimer() {
        val session = _state.value.session ?: return

        // Reset to original duration and restart
        val brewingTime = session.currentSteep.brewingTime
        val resetState = timerService.getCurrentState().copy(
            totalDuration = brewingTime,
            remainingDuration = brewingTime,
            status = TimerStatus.NOT_STARTED,
            startedAt = null,
            pausedAt = null,
        )
        timerService.updateState(resetState)

        // Start timer again
        startTimer()
    }

}

sealed interface TimerNavEvent {
    data object NavigateBack : TimerNavEvent
    data class NavigateToSteepComplete(val sessionId: String) : TimerNavEvent
    data class NavigateToSessionTimer(val sessionId: String) : TimerNavEvent
}
