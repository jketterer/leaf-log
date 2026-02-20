package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateAverageRatingUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateTeaStatsUseCase
import dev.jketterer.leaflog.domain.usecases.timer.AdjustTimeUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CancelTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.CompleteTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.PauseTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.RestoreTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.ResumeTimerUseCase
import dev.jketterer.leaflog.domain.usecases.timer.SaveTimerStateUseCase
import dev.jketterer.leaflog.domain.usecases.timer.StartTimerUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.createConfigurationSaveDelegate
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TimerViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val imageStorage: ImageStorage,
    private val timerService: TimerService,
    private val addSteepUseCase: AddSteepUseCase,
    private val updateAverageRatingUseCase: UpdateAverageRatingUseCase,
    private val startTimerUseCase: StartTimerUseCase,
    private val pauseTimerUseCase: PauseTimerUseCase,
    private val resumeTimerUseCase: ResumeTimerUseCase,
    private val adjustTimeUseCase: AdjustTimeUseCase,
    private val completeTimerUseCase: CompleteTimerUseCase,
    private val cancelTimerUseCase: CancelTimerUseCase,
    private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
    private val updateTeaStatsUseCase: UpdateTeaStatsUseCase,
    private val saveTimerStateUseCase: SaveTimerStateUseCase,
    private val restoreTimerStateUseCase: RestoreTimerStateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TimerScreenState())
    val state: StateFlow<TimerScreenState> = _state.asStateFlow()

    private val _navigationEvents = Channel<TimerNavEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private val configSaveDelegate = createConfigurationSaveDelegate(
        saveBrewingConfigurationUseCase = saveBrewingConfigurationUseCase,
        stateFlow = _state,
        getSavedSession = { it.savedSession },
        dismissDialog = { it.copy(showSaveConfigurationDialog = false) },
        setError = { state, error -> state.copy(error = error) },
        createSuccessNavEvent = { session -> TimerNavEvent.NavigateToComplete(session.id) },
        sendNavEvent = { _navigationEvents.trySend(it) },
    )

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
            is TimerIntent.StopTimer -> showStopConfirmation()
            is TimerIntent.ConfirmStop -> confirmStop()
            is TimerIntent.CancelStop -> cancelStop()
            is TimerIntent.RatingChanged -> updateRating(intent.rating)
            is TimerIntent.NotesChanged -> updateNotes(intent.notes)
            is TimerIntent.PhotoSelected -> addPhoto(intent.imageBytes)
            is TimerIntent.PhotoRemoved -> removePhoto(intent.path)
            is TimerIntent.ShowNextSteepDialog -> showNextSteepDialog()
            is TimerIntent.CancelNextSteepDialog -> cancelNextSteepDialog()
            is TimerIntent.UpdateNextSteepDuration -> updateNextSteepDuration(intent.duration)
            is TimerIntent.UpdateNextSteepTemperature -> updateNextSteepTemperature(intent.temperature)
            is TimerIntent.ToggleTemperatureUnit -> toggleTemperatureUnit()
            is TimerIntent.ToggleVolumeUnit -> toggleVolumeUnit()
            is TimerIntent.ConfirmNextSteep -> confirmNextSteep(intent.session)
            is TimerIntent.SaveAndFinish -> saveAndFinish(intent.session)

            is TimerIntent.RestartTimer -> restartTimer()
            is TimerIntent.BackClicked -> _navigationEvents.trySend(TimerNavEvent.NavigateBack)

            is TimerIntent.SaveConfigurationClicked ->
                configSaveDelegate.saveConfiguration(intent.customLabel)

            is TimerIntent.SkipSaveConfiguration ->
                configSaveDelegate.skipSaveConfiguration()

            is TimerIntent.DiscardSession -> showDiscardConfirmation()
            is TimerIntent.ConfirmDiscardSession -> confirmDiscardSession()
            is TimerIntent.CancelDiscardSession -> cancelDiscardConfirmation()

            is TimerIntent.EditSession -> openEditSheet()
            is TimerIntent.EditTemperatureChanged -> _state.update { it.copy(editTemperatureCelsius = intent.value) }
            is TimerIntent.EditWaterQuantityChanged -> _state.update { it.copy(editWaterQuantityMl = intent.value) }
            is TimerIntent.EditTeaQuantityChanged -> _state.update { it.copy(editTeaQuantityGrams = intent.value) }
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

                    // 3. Persist adjusted state so it survives process death
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

                    // 4. Clear persisted timer state
                    _state.value.session?.id?.let { sessionId ->
                        saveTimerStateUseCase.clear(sessionId)
                    }

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
                .onSuccess { newState ->
                    timerService.updateState(newState)
                    // Persist completed state so user can resume to completion screen
                    saveTimerStateUseCase(newState)
                }
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

    private fun updateNextSteepTemperature(temperature: Double) {
        _state.update { it.copy(nextSteepTemperature = temperature) }
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

        // Save current session with rating and notes, clear timer state
        // If this is the parent session, keep it IN_PROGRESS since the overall
        // session is still active (a new steep is being started)
        val isParent = session.parentSessionId == null
        val updatedSession = session.copy(
            status = if (isParent) SessionStatus.IN_PROGRESS else SessionStatus.COMPLETED,
            rating = _state.value.rating.takeIf { it > 0f },
            notes = _state.value.notes?.takeIf { it.isNotBlank() },
            photos = _state.value.photos,
            updatedAt = Clock.System.now(),
            timerStatus = null,
            timerStartedAt = null,
            timerPausedAt = null,
            timerRemainingMs = null,
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
                        // Preserve vessel as it doesn't change between steeps
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
        // Save current session with rating, notes, and photos
        // Clear timer state fields since session is being completed
        val updatedSession = session.copy(
            status = SessionStatus.COMPLETED,
            rating = state.value.rating.takeIf { it > 0f },
            notes = state.value.notes,
            photos = state.value.photos,
            updatedAt = Clock.System.now(),
            timerStatus = null,
            timerStartedAt = null,
            timerPausedAt = null,
            timerRemainingMs = null,
        )
        teaSessionRepository.upsert(updatedSession)

        // Update parent session's average rating and mark it as completed
        session.parentSessionId?.let { parentId ->
            updateAverageRatingUseCase(parentId)
            // Mark parent session as completed since the user is finishing the session
            val parentSession = teaSessionRepository.getById(parentId)
            parentSession?.let {
                teaSessionRepository.upsert(it.copy(
                    status = SessionStatus.COMPLETED,
                    updatedAt = Clock.System.now(),
                ))
            }
        }

        // Update tea stats (totalSessions, averageRating, lastBrewedAt)
        updateTeaStatsUseCase(session.teaId)

        // Check if we should show the save configuration dialog
        // Show for first steep (steepNumber == 1) with rating >= 3 stars
        // Skip if session already used a saved configuration
        if (updatedSession.steepNumber == 1 &&
            updatedSession.rating != null &&
            updatedSession.rating >= 3f &&
            updatedSession.usedConfigurationId == null
        ) {
            _state.update {
                it.copy(
                    showSaveConfigurationDialog = true,
                    savedSession = updatedSession
                )
            }
        } else {
            _navigationEvents.send(TimerNavEvent.NavigateToComplete(session.id))
        }
    }

    private fun showDiscardConfirmation() {
        _state.update { it.copy(showDiscardConfirmation = true) }
    }

    private fun cancelDiscardConfirmation() {
        _state.update { it.copy(showDiscardConfirmation = false) }
    }

    private fun confirmDiscardSession() {
        val session = _state.value.session ?: return
        _state.update { it.copy(showDiscardConfirmation = false, isLoading = true) }

        viewModelScope.launch {
            try {
                // Clean up any photos saved during this session
                for (photo in _state.value.photos) {
                    try {
                        imageStorage.deleteImage(photo)
                    } catch (_: Exception) {
                    }
                }

                // Delete the session from the database
                teaSessionRepository.delete(session.id)

                // If this was a child steep, update the parent's average rating
                // and mark parent as completed since the active steep was discarded
                session.parentSessionId?.let { parentId ->
                    updateAverageRatingUseCase(parentId)
                    val parentSession = teaSessionRepository.getById(parentId)
                    parentSession?.let {
                        teaSessionRepository.upsert(it.copy(
                            status = SessionStatus.COMPLETED,
                            updatedAt = Clock.System.now(),
                        ))
                    }
                }

                // Update tea stats (totalSessions, averageRating, lastBrewedAt)
                updateTeaStatsUseCase(session.teaId)

                // Clear timer state
                timerService.stop()
                saveTimerStateUseCase.clear(session.id)

                _navigationEvents.send(TimerNavEvent.NavigateBack)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to discard session: ${e.message}",
                    )
                }
            }
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

        val teaQuantityGrams = currentState.editTeaQuantityGrams.toFloatOrNull()

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

    @OptIn(ExperimentalUuidApi::class)
    private fun addPhoto(imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                val fileName = "${Uuid.random()}.jpg"
                val persistedPath = imageStorage.saveImage(imageBytes, fileName, "session_images")
                _state.update {
                    it.copy(photos = it.photos + persistedPath)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save photo: ${e.message}") }
            }
        }
    }

    private fun removePhoto(path: String) {
        viewModelScope.launch {
            try {
                imageStorage.deleteImage(path)
            } catch (_: Exception) {
            }
            _state.update {
                it.copy(photos = it.photos - path)
            }
        }
    }
}

sealed interface TimerNavEvent {
    data object NavigateBack : TimerNavEvent
    data class NavigateToNextSteep(val sessionId: String) : TimerNavEvent
    data class NavigateToComplete(val sessionId: String) : TimerNavEvent
}
