package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.timer.AdjustTimeUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CancelTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CompleteTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.PauseTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.RestoreTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.ResumeTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.SaveTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.StartTimerUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlin.time.Duration

class TimerViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val timerService: TimerService,
    private val startTimerUseCase: StartTimerUseCase,
    private val pauseTimerUseCase: PauseTimerUseCase,
    private val resumeTimerUseCase: ResumeTimerUseCase,
    private val adjustTimeUseCase: AdjustTimeUseCase,
    private val completeTimerUseCase: CompleteTimerUseCase,
    private val cancelTimerUseCase: CancelTimerUseCase,
    private val saveTimerStateUseCase: SaveTimerStateUseCase,
    private val restoreTimerStateUseCase: RestoreTimerStateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TimerScreenState())
    val state: StateFlow<TimerScreenState> = _state.asStateFlow()

    private val _navigationEvents = Channel<TimerNavEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
        collectTimerState()
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
            is TimerIntent.Initialize -> initialize(intent.sessionId)
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
            is TimerIntent.ToggleTemperatureUnit -> toggleTemperatureUnit()
            is TimerIntent.ToggleVolumeUnit -> toggleVolumeUnit()
            is TimerIntent.RestartTimer -> restartTimer()
            is TimerIntent.CompleteNow -> _state.update { it.copy(showCompleteNowConfirmation = true) }
            is TimerIntent.ConfirmCompleteNow -> confirmCompleteNow()
            is TimerIntent.CancelCompleteNow -> _state.update { it.copy(showCompleteNowConfirmation = false) }
            is TimerIntent.BackClicked -> _navigationEvents.trySend(TimerNavEvent.NavigateBack)

            is TimerIntent.EditSession -> openEditSheet()
            is TimerIntent.EditTemperatureChanged -> _state.update { it.copy(editTemperatureCelsius = intent.value) }
            is TimerIntent.EditWaterQuantityChanged -> _state.update { it.copy(editWaterQuantityMl = intent.value) }
            is TimerIntent.EditTeaQuantityChanged -> _state.update { it.copy(editTeaQuantityGrams = intent.value) }
            is TimerIntent.EditTeaBagModeChanged -> _state.update {
                it.copy(
                    editIsTeaBag = intent.isTeaBag,
                    editTeaQuantityGrams = if (intent.isTeaBag) "" else it.editTeaQuantityGrams,
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
                        timerState.sessionId == currentSession.id
                    ) {
                        handleTimerComplete()
                    }
                }
        }
    }

    private fun initialize(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val session = teaSessionRepository.getById(sessionId)
                if (session == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Session not found",
                        )
                    }
                    return@launch
                }

                val tea = teaRepository.getById(session.teaId)
                val vessel = brewingVesselRepository.getById(session.vesselId)

                // Prefer live TimerService state if it's already tracking this session
                // (e.g., navigating back while timer is still running)
                val currentTimerState = timerService.getCurrentState()
                val initialTimerState = if (currentTimerState.sessionId == sessionId &&
                    currentTimerState.status != TimerStatus.NOT_STARTED
                ) {
                    currentTimerState.copy(teaName = tea?.name ?: "")
                } else {
                    // Fall back to restoring from database (e.g., app was killed)
                    val restoredTimerState = restoreTimerStateUseCase(sessionId).getOrNull()
                    restoredTimerState?.copy(teaName = tea?.name ?: "")
                        ?: TimerState(
                            sessionId = session.id,
                            teaId = session.teaId,
                            teaName = tea?.name ?: "",
                            steepNumber = session.steepNumber,
                            totalDuration = session.brewingTime,
                            remainingDuration = session.brewingTime,
                        )
                }

                // If timer was already complete (e.g., app killed during SteepComplete),
                // redirect to SteepComplete immediately
                if (initialTimerState.status == TimerStatus.COMPLETE) {
                    _navigationEvents.send(TimerNavEvent.NavigateToSteepComplete(sessionId))
                    return@launch
                }

                // Update timer service with restored/initial state
                timerService.updateState(initialTimerState)

                // If timer was running, restart the countdown
                if (initialTimerState.status == TimerStatus.RUNNING) {
                    timerService.startCountdown()
                }

                _state.update {
                    it.copy(
                        session = session,
                        tea = tea,
                        vessel = vessel,
                        timerState = initialTimerState,
                        isLoading = false,
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load session: ${e.message}",
                    )
                }
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
                .onFailure { e ->
                    _state.update {
                        it.copy(error = "Failed to start timer: ${e.message}")
                    }
                }
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
                .onFailure { e ->
                    _state.update {
                        it.copy(error = "Failed to pause timer: ${e.message}")
                    }
                }
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
                .onFailure { e ->
                    _state.update {
                        it.copy(error = "Failed to resume timer: ${e.message}")
                    }
                }
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
                .onFailure { e ->
                    _state.update {
                        it.copy(error = "Failed to adjust timer: ${e.message}")
                    }
                }
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
        val resetState = timerService.getCurrentState().copy(
            totalDuration = session.brewingTime,
            remainingDuration = session.brewingTime,
            status = TimerStatus.NOT_STARTED,
            startedAt = null,
            pausedAt = null,
        )
        timerService.updateState(resetState)
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
            // 1. Call use case - handles cleanup logic
            cancelTimerUseCase(timerService.getCurrentState())
                .onSuccess { newState ->
                    // 2. Update TimerService
                    timerService.updateState(newState)

                    // 3. Stop countdown
                    timerService.stop()

                    // 4. Clear persisted timer state
                    saveTimerStateUseCase.clear(session.id)

                    // 5. Delete the session from the database
                    teaSessionRepository.delete(session.id)

                    _navigationEvents.send(TimerNavEvent.NavigateBack)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(error = "Failed to discard session: ${e.message}")
                    }
                }
        }
    }

    private fun handleTimerComplete() {
        val session = _state.value.session ?: return

        viewModelScope.launch {
            completeTimerUseCase(
                currentState = timerService.getCurrentState(),
                session = session,
            )
                .onSuccess { newState ->
                    timerService.updateState(newState)
                    // Persist completed state for restoration
                    saveTimerStateUseCase(newState)
                    // Reset singleton state so the next Timer screen (e.g., next steep) starts
                    // clean and doesn't receive a stale COMPLETE emission in collectTimerState()
                    timerService.stop()
                    // Navigate to SteepComplete screen
                    _navigationEvents.send(TimerNavEvent.NavigateToSteepComplete(session.id))
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to complete timer: ${e.message}") }
                }
        }
    }

    private fun toggleTemperatureUnit() {
        viewModelScope.launch {
            val currentUnit = _state.value.userPreferences.temperatureUnit
            val newUnit = currentUnit.toggle()

            // Reconvert the edit field value if the edit sheet is open
            if (_state.value.showEditSheet) {
                val currentDisplayValue = _state.value.editTemperatureCelsius.toIntOrNull()
                if (currentDisplayValue != null) {
                    val celsius = currentUnit.toCelsius(currentDisplayValue)
                    val newDisplayValue = newUnit.fromCelsius(celsius)
                    _state.update { it.copy(editTemperatureCelsius = newDisplayValue.toString()) }
                }
            }

            preferencesRepository.updateTemperatureUnit(newUnit)
        }
    }

    private fun toggleVolumeUnit() {
        viewModelScope.launch {
            val currentUnit = _state.value.userPreferences.volumeUnit
            val newUnit = currentUnit.toggle()

            // Reconvert the edit field value if the edit sheet is open
            if (_state.value.showEditSheet) {
                val currentDisplayValue = _state.value.editWaterQuantityMl.toIntOrNull()
                if (currentDisplayValue != null) {
                    val ml = currentUnit.toMilliliters(currentDisplayValue)
                    val newDisplayValue = newUnit.fromMilliliters(ml)
                    _state.update { it.copy(editWaterQuantityMl = newDisplayValue.toString()) }
                }
            }

            preferencesRepository.updateVolumeUnit(newUnit)
        }
    }

    private fun openEditSheet() {
        val session = _state.value.session ?: return
        val tempUnit = _state.value.userPreferences.temperatureUnit
        val volUnit = _state.value.userPreferences.volumeUnit
        _state.update {
            it.copy(
                showEditSheet = true,
                editTemperatureCelsius = tempUnit.fromCelsius(session.temperatureCelsius)
                    .toString(),
                editWaterQuantityMl = volUnit.fromMilliliters(session.waterQuantityMl).toString(),
                editTeaQuantityGrams = session.teaQuantityGrams?.toString() ?: "",
                editIsTeaBag = session.teaQuantityGrams == null,
                editWaterType = session.waterType,
            )
        }
    }

    private fun confirmEditSession() {
        val session = _state.value.session ?: return
        val currentState = _state.value
        val tempUnit = currentState.userPreferences.temperatureUnit
        val volUnit = currentState.userPreferences.volumeUnit

        val temperatureCelsius = currentState.editTemperatureCelsius.toIntOrNull()?.let {
            tempUnit.toCelsius(it)
        } ?: session.temperatureCelsius

        val waterQuantityMl = currentState.editWaterQuantityMl.toIntOrNull()?.let {
            volUnit.toMilliliters(it)
        } ?: session.waterQuantityMl

        val teaQuantityGrams = if (currentState.editIsTeaBag) null
        else currentState.editTeaQuantityGrams.toFloatOrNull()

        val waterType = currentState.editWaterType ?: session.waterType

        val updatedSession = session.copy(
            temperatureCelsius = temperatureCelsius,
            waterQuantityMl = waterQuantityMl,
            teaQuantityGrams = teaQuantityGrams,
            waterType = waterType,
            updatedAt = Clock.System.now(),
        )

        _state.update {
            it.copy(
                showEditSheet = false,
                session = updatedSession,
            )
        }

        viewModelScope.launch {
            teaSessionRepository.upsert(updatedSession)
        }
    }

    private fun confirmCompleteNow() {
        _state.update { it.copy(showCompleteNowConfirmation = false) }
        handleTimerComplete()
    }

    private fun restartTimer() {
        val session = _state.value.session ?: return

        // Reset to original duration and restart
        val resetState = timerService.getCurrentState().copy(
            totalDuration = session.brewingTime,
            remainingDuration = session.brewingTime,
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
