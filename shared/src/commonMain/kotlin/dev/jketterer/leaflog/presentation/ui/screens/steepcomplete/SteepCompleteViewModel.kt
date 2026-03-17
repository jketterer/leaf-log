package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.services.TimerService
import dev.jketterer.leaflog.domain.usecases.configuration.CheckDuplicateConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateAverageRatingUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateTeaStatsUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.createConfigurationSaveDelegate
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SteepCompleteViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val imageStorage: ImageStorage,
    private val timerService: TimerService,
    private val addSteepUseCase: AddSteepUseCase,
    private val updateAverageRatingUseCase: UpdateAverageRatingUseCase,
    private val updateTeaStatsUseCase: UpdateTeaStatsUseCase,
    private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
    private val checkDuplicateConfigurationUseCase: CheckDuplicateConfigurationUseCase,
    private val generateConfigurationLabelUseCase: GenerateConfigurationLabelUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SteepCompleteState())
    val state: StateFlow<SteepCompleteState> = _state.asStateFlow()

    private val _navigationEvents = Channel<SteepCompleteNavEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private val configSaveDelegate = createConfigurationSaveDelegate(
        saveBrewingConfigurationUseCase = saveBrewingConfigurationUseCase,
        stateFlow = _state,
        getSavedSession = { it.savedSession },
        dismissDialog = { it.copy(showSaveConfigurationDialog = false) },
        setError = { state, error -> state.copy(error = error) },
        createSuccessNavEvent = { _ -> SteepCompleteNavEvent.NavigateToHome },
        sendNavEvent = { _navigationEvents.trySend(it) },
    )

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
    }

    fun onIntent(intent: SteepCompleteIntent) {
        when (intent) {
            is SteepCompleteIntent.Initialize -> initialize(intent.sessionId)
            is SteepCompleteIntent.RatingChanged -> updateRating(intent.rating)
            is SteepCompleteIntent.NotesChanged -> updateNotes(intent.notes)
            is SteepCompleteIntent.PhotoSelected -> addPhoto(intent.imageBytes)
            is SteepCompleteIntent.PhotoRemoved -> removePhoto(intent.path)
            is SteepCompleteIntent.ShowNextSteepDialog -> showNextSteepDialog()
            is SteepCompleteIntent.DismissNextSteepDialog -> dismissNextSteepDialog()
            is SteepCompleteIntent.NextSteepDurationChanged -> _state.update {
                it.copy(nextSteepDuration = intent.duration)
            }

            is SteepCompleteIntent.NextSteepTemperatureChanged -> _state.update {
                it.copy(nextSteepTemperature = intent.temperature)
            }

            is SteepCompleteIntent.NextSteepWaterQuantityChanged -> _state.update {
                it.copy(nextSteepWaterQuantity = intent.waterQuantityMl)
            }

            is SteepCompleteIntent.ConfirmNextSteep -> confirmNextSteep()
            is SteepCompleteIntent.FinishSession -> finishSession()
            is SteepCompleteIntent.SaveConfigurationClicked ->
                configSaveDelegate.saveConfiguration(intent.customLabel)

            is SteepCompleteIntent.SkipSaveConfiguration ->
                configSaveDelegate.skipSaveConfiguration()

            is SteepCompleteIntent.ShowDiscardConfirmation -> _state.update {
                it.copy(showDiscardConfirmation = true)
            }

            is SteepCompleteIntent.DismissDiscardConfirmation -> _state.update {
                it.copy(showDiscardConfirmation = false)
            }

            is SteepCompleteIntent.ConfirmDiscard -> confirmDiscardSession()
            is SteepCompleteIntent.PreviousSteepRatingChanged ->
                updatePreviousSteepRating(intent.steepId, intent.rating)

            is SteepCompleteIntent.PreviousSteepNotesChanged ->
                updatePreviousSteepNotes(intent.steepId, intent.notes)

            is SteepCompleteIntent.ToggleTemperatureUnit -> toggleTemperatureUnit()
            is SteepCompleteIntent.BackClicked -> _navigationEvents.trySend(SteepCompleteNavEvent.NavigateBack)
            is SteepCompleteIntent.ShowEditParametersSheet -> openEditParametersSheet()
            is SteepCompleteIntent.DismissEditParametersSheet -> _state.update { it.copy(showEditParametersSheet = false) }
            is SteepCompleteIntent.EditBrewingTimeChanged -> _state.update { it.copy(editBrewingTime = intent.duration) }
            is SteepCompleteIntent.EditTemperatureChanged -> _state.update { it.copy(editTemperatureCelsius = intent.value) }
            is SteepCompleteIntent.EditWaterQuantityChanged -> _state.update { it.copy(editWaterQuantityMl = intent.value) }
            is SteepCompleteIntent.EditTeaQuantityChanged -> _state.update { it.copy(editTeaQuantityGrams = intent.value) }
            is SteepCompleteIntent.EditTeaBagModeChanged -> _state.update { it.copy(editIsTeaBag = intent.isTeaBag) }
            is SteepCompleteIntent.EditWaterTypeChanged -> _state.update { it.copy(editWaterType = intent.waterType) }
            is SteepCompleteIntent.ConfirmEditParameters -> confirmEditParameters()
            is SteepCompleteIntent.CancelEditParameters -> _state.update { it.copy(showEditParametersSheet = false) }
            is SteepCompleteIntent.ToggleVolumeUnit -> toggleVolumeUnit()
        }
    }

    private fun initialize(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val session = teaSessionRepository.getById(sessionId)
                if (session == null) {
                    _state.update { it.copy(isLoading = false, error = "Session not found") }
                    return@launch
                }

                val teaDeferred = async { teaRepository.getById(session.teaId) }
                val vesselDeferred = async { brewingVesselRepository.getById(session.vesselId) }
                val tea = teaDeferred.await()
                val vessel = vesselDeferred.await()

                // Load previous steeps
                val previousSteeps = loadPreviousSteeps(session)

                // Pre-fill rating from tea's average if the session has no rating yet
                val suggestedRating = if (session.rating == null && tea?.averageRating != null) {
                    kotlin.math.round(tea.averageRating).coerceIn(1f, 5f)
                } else {
                    null
                }

                if (suggestedRating != null) {
                    teaSessionRepository.upsert(
                        session.copy(rating = suggestedRating, updatedAt = Clock.System.now()),
                    )
                }

                _state.update {
                    it.copy(
                        session = session,
                        tea = tea,
                        vessel = vessel,
                        rating = session.rating ?: suggestedRating ?: 0f,
                        notes = session.notes ?: "",
                        photos = session.photos,
                        previousSteeps = previousSteeps,
                        nextSteepDuration = session.brewingTime,
                        nextSteepTemperature = session.temperatureCelsius,
                        nextSteepWaterQuantity = session.waterQuantityMl,
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

    private suspend fun loadPreviousSteeps(session: TeaSession): List<TeaSession> {
        val parentId = session.parentSessionId ?: return emptyList()

        val parent = teaSessionRepository.getById(parentId) ?: return emptyList()
        val children = teaSessionRepository.getChildSteeps(parentId)
            .filter { it.steepNumber < session.steepNumber }

        return (listOf(parent) + children).sortedBy { it.steepNumber }
    }

    private fun updateRating(rating: Float) {
        val session = _state.value.session ?: return
        _state.update { it.copy(rating = rating) }
        viewModelScope.launch {
            teaSessionRepository.upsert(
                session.copy(rating = rating.takeIf { it > 0f }, updatedAt = Clock.System.now())
            )
        }
    }

    private fun updateNotes(notes: String) {
        val session = _state.value.session ?: return
        _state.update { it.copy(notes = notes) }
        viewModelScope.launch {
            teaSessionRepository.upsert(
                session.copy(
                    notes = notes.takeIf { it.isNotBlank() },
                    updatedAt = Clock.System.now()
                )
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun addPhoto(imageBytes: ByteArray) {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            try {
                val fileName = "${Uuid.random()}.jpg"
                val persistedPath = imageStorage.saveImage(imageBytes, fileName, "session_images")
                val newPhotos = _state.value.photos + persistedPath
                _state.update { it.copy(photos = newPhotos) }
                teaSessionRepository.upsert(
                    session.copy(photos = newPhotos, updatedAt = Clock.System.now())
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save photo: ${e.message}") }
            }
        }
    }

    private fun removePhoto(path: String) {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            try {
                imageStorage.deleteImage(path)
            } catch (_: Exception) {
            }
            val newPhotos = _state.value.photos - path
            _state.update { it.copy(photos = newPhotos) }
            teaSessionRepository.upsert(
                session.copy(photos = newPhotos, updatedAt = Clock.System.now())
            )
        }
    }

    private fun showNextSteepDialog() {
        val session = _state.value.session ?: return
        _state.update {
            it.copy(
                showNextSteepDialog = true,
                nextSteepDuration = it.nextSteepDuration ?: session.brewingTime,
                nextSteepTemperature = it.nextSteepTemperature ?: session.temperatureCelsius,
                nextSteepWaterQuantity = it.nextSteepWaterQuantity ?: session.waterQuantityMl,
            )
        }
    }

    private fun dismissNextSteepDialog() {
        _state.update { it.copy(showNextSteepDialog = false) }
    }

    private fun confirmNextSteep() {
        val session = _state.value.session ?: return
        val duration = _state.value.nextSteepDuration ?: session.brewingTime
        val temperature = _state.value.nextSteepTemperature ?: session.temperatureCelsius
        val waterQuantity = _state.value.nextSteepWaterQuantity ?: session.waterQuantityMl

        _state.update { it.copy(isLoading = true, showNextSteepDialog = false) }

        viewModelScope.launch {
            val updatedSession = session.copy(
                status = SessionStatus.COMPLETED,
                rating = _state.value.rating.takeIf { it > 0f },
                notes = _state.value.notes.takeIf { it.isNotBlank() },
                photos = _state.value.photos,
                updatedAt = Clock.System.now(),
                timerStatus = null,
                timerStartedAt = null,
                timerPausedAt = null,
                timerRemainingMs = null,
            )
            teaSessionRepository.upsert(updatedSession)

            session.parentSessionId?.let { parentId ->
                updateAverageRatingUseCase(parentId)
            }

            // Get root session (AddSteepUseCase requires root, not a child)
            val rootSession = if (session.parentSessionId != null) {
                teaSessionRepository.getById(session.parentSessionId) ?: updatedSession
            } else {
                updatedSession
            }

            addSteepUseCase(
                parentSession = rootSession,
                brewingTime = duration,
                temperatureCelsius = temperature,
                waterQuantityMl = waterQuantity,
            ).onSuccess { nextSteep ->
                _state.update { it.copy(isLoading = false) }
                // Reset TimerService singleton state before the next Timer screen starts
                timerService.stop()
                _navigationEvents.send(SteepCompleteNavEvent.NavigateToTimer(nextSteep.id))
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Unable to start next steep: ${e.message}",
                    )
                }
            }
        }
    }

    private fun finishSession() {
        val session = _state.value.session ?: return
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val updatedSession = session.copy(
                status = SessionStatus.COMPLETED,
                rating = _state.value.rating.takeIf { it > 0f },
                notes = _state.value.notes.takeIf { it.isNotBlank() },
                photos = _state.value.photos,
                updatedAt = Clock.System.now(),
                timerStatus = null,
                timerStartedAt = null,
                timerPausedAt = null,
                timerRemainingMs = null,
            )
            teaSessionRepository.upsert(updatedSession)
            // Reset TimerService singleton state so the next Timer screen starts clean
            timerService.stop()

            session.parentSessionId?.let { parentId ->
                updateAverageRatingUseCase(parentId)
                val parentSession = teaSessionRepository.getById(parentId)
                parentSession?.let {
                    teaSessionRepository.upsert(
                        it.copy(status = SessionStatus.COMPLETED, updatedAt = Clock.System.now())
                    )
                }
            }

            updateTeaStatsUseCase(session.teaId)

            // Check if we should show save config dialog
            val shouldPromptConfig = updatedSession.steepNumber == 1 &&
                    (updatedSession.rating ?: 0f) >= 3f &&
                    updatedSession.usedConfigurationId == null

            if (shouldPromptConfig) {
                maybeShowSaveConfigDialog(updatedSession)
            } else {
                _state.update { it.copy(isLoading = false) }
                _navigationEvents.send(SteepCompleteNavEvent.NavigateToHome)
            }
        }
    }

    private suspend fun maybeShowSaveConfigDialog(session: TeaSession) {
        if (checkDuplicateConfigurationUseCase(session)) {
            _state.update { it.copy(isLoading = false) }
            _navigationEvents.send(SteepCompleteNavEvent.NavigateToHome)
            return
        }
        val label = generateConfigurationLabelUseCase(
            vesselName = _state.value.vessel?.name ?: "",
            teaQuantityGrams = session.teaQuantityGrams,
            waterQuantityMl = session.waterQuantityMl,
            brewingTime = session.brewingTime,
        )
        _state.update {
            it.copy(
                isLoading = false,
                showSaveConfigurationDialog = true,
                suggestedConfigurationLabel = label,
                savedSession = session,
            )
        }
    }

    private fun confirmDiscardSession() {
        val session = _state.value.session ?: return
        _state.update { it.copy(showDiscardConfirmation = false, isLoading = true) }

        viewModelScope.launch {
            try {
                for (photo in _state.value.photos) {
                    try {
                        imageStorage.deleteImage(photo)
                    } catch (_: Exception) {
                    }
                }

                teaSessionRepository.delete(session.id)

                session.parentSessionId?.let { parentId ->
                    updateAverageRatingUseCase(parentId)
                    val parentSession = teaSessionRepository.getById(parentId)
                    parentSession?.let {
                        teaSessionRepository.upsert(
                            it.copy(
                                status = SessionStatus.COMPLETED,
                                updatedAt = Clock.System.now()
                            )
                        )
                    }
                }

                updateTeaStatsUseCase(session.teaId)

                _navigationEvents.send(SteepCompleteNavEvent.NavigateToHome)
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

    private fun updatePreviousSteepRating(steepId: String, rating: Float) {
        val session = _state.value.session ?: return
        val steep = _state.value.previousSteeps.find { it.id == steepId } ?: return
        val parentId = steep.parentSessionId ?: session.id

        viewModelScope.launch {
            teaSessionRepository.upsert(
                steep.copy(rating = rating.takeIf { it > 0f }, updatedAt = Clock.System.now())
            )
            updateAverageRatingUseCase(parentId)

            // Reload previous steeps to reflect updated averageRating on parent
            val updatedSteeps = loadPreviousSteeps(session)
            _state.update { it.copy(previousSteeps = updatedSteeps) }
        }
    }

    private fun updatePreviousSteepNotes(steepId: String, notes: String) {
        val steep = _state.value.previousSteeps.find { it.id == steepId } ?: return
        val updatedSteep = steep.copy(
            notes = notes.takeIf { it.isNotBlank() },
            updatedAt = Clock.System.now(),
        )
        _state.update {
            it.copy(
                previousSteeps = it.previousSteeps.map { s ->
                    if (s.id == steepId) updatedSteep else s
                },
            )
        }
        viewModelScope.launch {
            teaSessionRepository.upsert(updatedSteep)
        }
    }

    private fun toggleTemperatureUnit() {
        viewModelScope.launch {
            val currentUnit = _state.value.userPreferences.temperatureUnit
            val newUnit = currentUnit.toggle()
            preferencesRepository.updateTemperatureUnit(newUnit)
        }
    }

    private fun openEditParametersSheet() {
        val session = _state.value.session ?: return
        val tempUnit = _state.value.userPreferences.temperatureUnit
        val volUnit = _state.value.userPreferences.volumeUnit
        _state.update {
            it.copy(
                showEditParametersSheet = true,
                editBrewingTime = session.brewingTime,
                editTemperatureCelsius = tempUnit.fromCelsius(session.temperatureCelsius).toString(),
                editWaterQuantityMl = volUnit.fromMilliliters(session.waterQuantityMl).toString(),
                editTeaQuantityGrams = session.teaQuantityGrams?.toString() ?: "",
                editIsTeaBag = session.teaQuantityGrams == null,
                editWaterType = session.waterType,
            )
        }
    }

    private fun confirmEditParameters() {
        val session = _state.value.session ?: return
        val currentState = _state.value
        val tempUnit = currentState.userPreferences.temperatureUnit
        val volUnit = currentState.userPreferences.volumeUnit

        val temperatureCelsius = currentState.editTemperatureCelsius.toIntOrNull()
            ?.let { tempUnit.toCelsius(it) } ?: session.temperatureCelsius
        val waterQuantityMl = currentState.editWaterQuantityMl.toIntOrNull()
            ?.let { volUnit.toMilliliters(it) } ?: session.waterQuantityMl
        val teaQuantityGrams = if (currentState.editIsTeaBag) null
            else currentState.editTeaQuantityGrams.toFloatOrNull()
        val waterType = currentState.editWaterType ?: session.waterType
        val brewingTime = currentState.editBrewingTime.takeIf { it > Duration.ZERO }
            ?: session.brewingTime

        val updatedSession = session.copy(
            brewingTime = brewingTime,
            temperatureCelsius = temperatureCelsius,
            waterQuantityMl = waterQuantityMl,
            teaQuantityGrams = teaQuantityGrams,
            waterType = waterType,
            updatedAt = Clock.System.now(),
        )
        _state.update { it.copy(showEditParametersSheet = false, session = updatedSession) }
        viewModelScope.launch { teaSessionRepository.upsert(updatedSession) }
    }

    private fun toggleVolumeUnit() {
        viewModelScope.launch {
            val currentUnit = _state.value.userPreferences.volumeUnit
            val newUnit = currentUnit.toggle()
            val currentDisplayValue = _state.value.editWaterQuantityMl.toIntOrNull()
            if (currentDisplayValue != null) {
                val ml = currentUnit.toMilliliters(currentDisplayValue)
                val newDisplayValue = newUnit.fromMilliliters(ml)
                _state.update { it.copy(editWaterQuantityMl = newDisplayValue.toString()) }
            }
            preferencesRepository.updateVolumeUnit(newUnit)
        }
    }
}

sealed interface SteepCompleteNavEvent {
    data object NavigateToHome : SteepCompleteNavEvent
    data class NavigateToTimer(val sessionId: String) : SteepCompleteNavEvent
    data object NavigateBack : SteepCompleteNavEvent
}
