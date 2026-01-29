package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.timer.AdjustTimeUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CancelTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CompleteTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.PauseTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.ResumeTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.StartTimerUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration

class TimerViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val timerService: TimerService,
    private val addSteepUseCase: AddSteepUseCase,
    private val startTimerUseCase: StartTimerUseCase,
    private val pauseTimerUseCase: PauseTimerUseCase,
    private val resumeTimerUseCase: ResumeTimerUseCase,
    private val adjustTimeUseCase: AdjustTimeUseCase,
    private val completeTimerUseCase: CompleteTimerUseCase,
    private val cancelTimerUseCase: CancelTimerUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TimerScreenState())
    val state: StateFlow<TimerScreenState> = _state.asStateFlow()

    private val _navigationEvents = Channel<TimerNavEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        collectTimerState()
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
            is TimerIntent.StopTimer -> showStopConfirmation()
            is TimerIntent.ConfirmStop -> confirmStop()
            is TimerIntent.CancelStop -> cancelStop()
            is TimerIntent.RatingChanged -> updateRating(intent.rating)
            is TimerIntent.NotesChanged -> updateNotes(intent.notes)
            is TimerIntent.AddPhotoClicked -> {
                // Photo picker handled by UI
            }

            is TimerIntent.PhotoSelected -> addPhoto(intent.photoUri)
            is TimerIntent.PhotoRemoved -> removePhoto(intent.photoUri)
            is TimerIntent.ShowNextSteepDialog -> showNextSteepDialog()
            is TimerIntent.CancelNextSteepDialog -> cancelNextSteepDialog()
            is TimerIntent.UpdateNextSteepDuration -> updateNextSteepDuration(intent.duration)
            is TimerIntent.UpdateNextSteepTemperature -> updateNextSteepTemperature(intent.temperature)
            is TimerIntent.UpdateNextSteepWaterQuantity -> updateNextSteepWaterQuantity(intent.quantity)
            is TimerIntent.ConfirmNextSteep -> confirmNextSteep(intent.session)
            is TimerIntent.SaveAndFinish -> saveAndFinish(intent.session)

            is TimerIntent.RestartTimer -> restartTimer()
            is TimerIntent.BackClicked -> _navigationEvents.trySend(TimerNavEvent.NavigateBack)
        }
    }

    private fun collectTimerState() {
        viewModelScope.launch {
            timerService.timerState
                .collect { timerState ->
                    _state.update { it.copy(timerState = timerState) }

                    // Auto-complete when timer reaches zero
                    if (timerState.status == TimerStatus.COMPLETE && _state.value.session != null) {
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
                _state.update {
                    it.copy(
                        session = session,
                        tea = tea,
                        timerState = TimerState(),
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

        viewModelScope.launch {
            startTimerUseCase(
                session = session,
                duration = session.brewingTime,
            )
                .onSuccess { timerState ->
                    val updatedState = timerState.copy(
                        teaName = _state.value.tea?.name ?: "",
                    )

                    timerService.updateState(updatedState)
                    timerService.startCountdown()
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

    private fun showStopConfirmation() {
        _state.update { it.copy(showStopConfirmation = true) }
    }

    private fun cancelStop() {
        _state.update { it.copy(showStopConfirmation = false) }
    }

    private fun confirmStop() {
        _state.update { it.copy(showStopConfirmation = false) }

        viewModelScope.launch {
            // 1. Call use case - handles cleanup logic
            cancelTimerUseCase(timerService.getCurrentState())
                .onSuccess { newState ->
                    // 2. Update TimerService
                    timerService.updateState(newState)

                    // 3. Stop countdown
                    timerService.stop()

                    _navigationEvents.send(TimerNavEvent.NavigateBack)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(error = "Failed to stop timer: ${e.message}")
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
                .onSuccess { newState -> timerService.updateState(newState) }
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to complete timer: ${e.message}") }
                }
        }
    }

    private fun showNextSteepDialog() {
        val session = _state.value.session ?: return
        _state.update {
            it.copy(
                showNextSteepDialog = true,
                nextSteepDuration = session.brewingTime,
                nextSteepTemperature = session.temperatureCelsius,
                nextSteepWaterQuantity = session.waterQuantityMl,
            )
        }
    }

    private fun cancelNextSteepDialog() {
        _state.update {
            it.copy(
                showNextSteepDialog = false,
                nextSteepDuration = null,
                nextSteepTemperature = null,
                nextSteepWaterQuantity = null,
            )
        }
    }

    private fun updateNextSteepDuration(duration: Duration?) {
        _state.update { it.copy(nextSteepDuration = duration) }
    }

    private fun updateNextSteepTemperature(temperature: Int) {
        _state.update { it.copy(nextSteepTemperature = temperature) }
    }

    private fun updateNextSteepWaterQuantity(quantity: Int) {
        _state.update { it.copy(nextSteepWaterQuantity = quantity) }
    }

    private fun confirmNextSteep(session: TeaSession) = viewModelScope.launch {
        val duration = _state.value.nextSteepDuration ?: session.brewingTime
        val temperature = _state.value.nextSteepTemperature ?: session.temperatureCelsius
        val waterQuantity = _state.value.nextSteepWaterQuantity ?: session.waterQuantityMl

        _state.update {
            it.copy(
                isLoading = true,
                showNextSteepDialog = false,
            )
        }

        // Save current session with rating and notes
        val updatedSession = session.copy(
            rating = _state.value.rating.takeIf { it > 0f },
            notes = _state.value.notes?.takeIf { it.isNotBlank() },
            photos = _state.value.photos,
            updatedAt = Clock.System.now(),
        )
        teaSessionRepository.upsert(updatedSession)

        // Find the root session (the one without a parentSessionId)
        // AddSteepUseCase requires the root session, not a child
        val rootSession = if (session.parentSessionId != null) {
            teaSessionRepository.getById(session.parentSessionId) ?: session
        } else {
            updatedSession
        }

        // Create next steep with new parameters
        addSteepUseCase(
            parentSession = rootSession,
            brewingTime = duration,
            temperatureCelsius = temperature,
            waterQuantityMl = waterQuantity,
        )
            .onSuccess { nextSession ->
                // Load the new session and reset UI state
                val tea = teaRepository.getById(nextSession.teaId)
                val newTimerState = TimerState(
                    sessionId = nextSession.id,
                    teaId = nextSession.teaId,
                    teaName = tea?.name ?: "",
                    steepNumber = nextSession.steepNumber,
                    totalDuration = nextSession.brewingTime,
                    remainingDuration = nextSession.brewingTime,
                    status = TimerStatus.NOT_STARTED,
                )

                // Update timer service with new state (this will propagate via timerState flow)
                timerService.updateState(newTimerState)

                // Also update ViewModel state directly to ensure immediate UI update
                _state.update {
                    it.copy(
                        session = nextSession,
                        tea = tea,
                        timerState = newTimerState,
                        rating = 0f,
                        notes = null,
                        photos = emptyList(),
                        isLoading = false,
                    )
                }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Unable to progress to next steep: ${e.message}"
                    )
                }
            }
    }

    private fun saveAndFinish(session: TeaSession) = viewModelScope.launch {
        val updatedSession = session.copy(
            status = SessionStatus.COMPLETED,
            notes = state.value.notes,
            photos = state.value.photos,
            updatedAt = Clock.System.now(),
        )
        teaSessionRepository.upsert(updatedSession)
        _navigationEvents.send(TimerNavEvent.NavigateToComplete(session.id))
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

    private fun updateRating(rating: Float) {
        _state.update { it.copy(rating = rating) }
    }

    private fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    private fun addPhoto(photoUri: String) {
        _state.update {
            it.copy(photos = it.photos + photoUri)
        }
    }

    private fun removePhoto(photoUri: String) {
        _state.update {
            it.copy(photos = it.photos - photoUri)
        }
    }
}

sealed interface TimerNavEvent {
    data object NavigateBack : TimerNavEvent
    data class NavigateToNextSteep(val sessionId: String) : TimerNavEvent
    data class NavigateToComplete(val sessionId: String) : TimerNavEvent
}
