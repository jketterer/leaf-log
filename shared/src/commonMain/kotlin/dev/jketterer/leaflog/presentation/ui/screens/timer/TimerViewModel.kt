package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.session.DiscardSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.SaveTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.AdjustTimeUseCase
import dev.jketterer.leaflog.domain.usecases.timer.PauseTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.RestoreTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.ResumeTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.StartTimerUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_adjust_timer
import leaf_log.shared.generated.resources.couldnt_discard_steep
import leaf_log.shared.generated.resources.couldnt_load_session
import leaf_log.shared.generated.resources.couldnt_pause_timer
import leaf_log.shared.generated.resources.couldnt_resume_timer
import leaf_log.shared.generated.resources.couldnt_save_changes
import leaf_log.shared.generated.resources.couldnt_start_timer
import leaf_log.shared.generated.resources.failure_not_found
import kotlin.time.Clock
import kotlin.time.Duration

class TimerViewModel(
    private val route: NavRoute.TimerRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
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
) : ViewModel() {

    private val _state = MutableStateFlow(TimerScreenState())
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
        initialize(route.sessionId)
    }

    override fun onCleared() {
        super.onCleared()
        // Save timer state if timer is running or paused
        val currentState = timerService.getCurrentState()
        if (currentState.status == TimerStatus.RUNNING || currentState.status == TimerStatus.PAUSED) {
            // Use runBlocking since we need to complete this before ViewModel is destroyed
            // This is acceptable in onCleared as it's called during cleanup
            runBlocking {
                saveTimerStateUseCase(currentState)
            }
        }
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
            is TimerIntent.CompleteNow -> _state.update { it.copy(showCompleteNowConfirmation = true) }
            is TimerIntent.ConfirmCompleteNow -> confirmCompleteNow()
            is TimerIntent.CancelCompleteNow -> _state.update { it.copy(showCompleteNowConfirmation = false) }
            is TimerIntent.DismissNotificationWarning -> _state.update { it.copy(showNotificationWarning = false) }
            is TimerIntent.BackClicked -> _navigationEvents.trySend(TimerNavEvent.NavigateBack)

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

    private fun startTimer() {
        val session = _state.value.session ?: return
        val brewingTime = _state.value.timerState.totalDuration

        viewModelScope.launch {
            startTimerUseCase(
                session = session,
                duration = brewingTime,
            )
                .onSuccess { timerState ->
                    val updatedState = timerState.copy(
                        teaName = _state.value.tea?.name ?: "",
                    )

                    timerService.updateState(updatedState)
                    timerService.startCountdown()

                    // Persist running state for restoration
                    saveTimerStateUseCase(updatedState)
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
                    saveTimerStateUseCase(newState)
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
                    saveTimerStateUseCase(newState)
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
        val session = _state.value.session ?: return
        _state.update { it.copy(showResetConfirmation = false) }

        // Reset is simple state change - no use case needed
        val brewingTime = session.currentSteep.brewingTime
        val resetState = timerService.getCurrentState().copy(
            totalDuration = brewingTime,
            remainingDuration = brewingTime,
            status = TimerStatus.NOT_STARTED,
            startedAt = null,
            pausedAt = null,
        )
        timerService.updateState(resetState)

        // Persist so leaving the screen or losing the process doesn't restore the paused timer
        viewModelScope.launch { saveTimerStateUseCase(resetState) }
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

    private fun confirmCompleteNow() {
        _state.update { it.copy(showCompleteNowConfirmation = false) }
        handleTimerComplete()
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
}
