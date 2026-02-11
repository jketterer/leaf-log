package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.UpdateSessionUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.createConfigurationSaveDelegate
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

class EditSessionViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val updateSessionUseCase: UpdateSessionUseCase,
    private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditSessionState())
    val state: StateFlow<EditSessionState> = _state.asStateFlow()

    private val _navEvents = Channel<EditSessionNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    private val configSaveDelegate = createConfigurationSaveDelegate(
        saveBrewingConfigurationUseCase = saveBrewingConfigurationUseCase,
        stateFlow = _state,
        getSavedSession = { it.savedSession },
        dismissDialog = { it.copy(showSaveConfigurationDialog = false) },
        setError = { state, error -> state.copy(error = error) },
        createSuccessNavEvent = { EditSessionNavEvent.NavigateBack },
        sendNavEvent = { _navEvents.trySend(it) },
    )

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
        loadVessels()
    }

    fun onIntent(intent: EditSessionIntent) {
        when (intent) {
            is EditSessionIntent.LoadSession -> loadSession(
                intent.sessionId,
                intent.editFullSession
            )

            is EditSessionIntent.BrewingTimeChanged -> updateBrewingTime(intent.duration)
            is EditSessionIntent.TemperatureChanged -> updateTemperature(intent.temperature)
            is EditSessionIntent.VesselSelected -> selectVessel(intent.vessel)
            is EditSessionIntent.WaterTypeSelected -> selectWaterType(intent.waterType)
            is EditSessionIntent.WaterQuantityChanged -> updateWaterQuantity(intent.quantity)
            is EditSessionIntent.TeaQuantityChanged -> updateTeaQuantity(intent.quantity)
            is EditSessionIntent.LocationChanged -> updateLocation(intent.location)
            is EditSessionIntent.RatingChanged -> updateRating(intent.rating)
            is EditSessionIntent.NotesChanged -> updateNotes(intent.notes)
            is EditSessionIntent.AddPhotoClicked -> {
                // Photo picker handled by UI
            }

            is EditSessionIntent.PhotoSelected -> addPhoto(intent.photoUri)
            is EditSessionIntent.PhotoRemoved -> removePhoto(intent.photoUri)
            is EditSessionIntent.SaveClicked -> save()
            is EditSessionIntent.BackClicked -> handleBack()
            is EditSessionIntent.ConfirmDiscard -> confirmDiscard()
            is EditSessionIntent.CancelDiscard -> cancelDiscard()
            is EditSessionIntent.SaveConfigurationClicked ->
                configSaveDelegate.saveConfiguration(intent.customLabel)

            is EditSessionIntent.SkipSaveConfiguration ->
                configSaveDelegate.skipSaveConfiguration()
        }
    }

    private fun loadVessels() {
        viewModelScope.launch {
            brewingVesselRepository.getAllFlow()
                .catch { e ->
                    println("Failed to load vessels: ${e.message}")
                }
                .collect { vessels ->
                    _state.update { it.copy(availableVessels = vessels) }
                }
        }
    }

    private fun loadSession(sessionId: String, editFullSession: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val session = teaSessionRepository.getById(sessionId)

                if (session != null) {
                    // Load tea name for SaveConfigurationDialog
                    val tea = teaRepository.getById(session.teaId)
                    // Use editFullSession flag to determine whether to show parent fields
                    // When true: edit full session (from top app bar)
                    // When false: edit only steep parameters (from steep card)
                    val isParent = editFullSession

                    if (_state.value.availableVessels.isEmpty()) {
                        val vessels = brewingVesselRepository.getAll()
                        _state.update { it.copy(availableVessels = vessels) }
                    }
                    // Find vessel
                    val vessel =
                        _state.value.availableVessels.firstOrNull { it.id == session.vesselId }

                    _state.update {
                        it.copy(
                            existingSession = session,
                            isParentSession = isParent,
                            steepNumber = session.steepNumber,
                            teaName = tea?.name,
                            brewingTime = session.brewingTime,
                            temperatureCelsius = session.temperatureCelsius.toString(),
                            selectedVessel = vessel,
                            selectedWaterType = session.waterType,
                            waterQuantityMl = session.waterQuantityMl.toString(),
                            teaQuantityGrams = session.teaQuantityGrams?.toString() ?: "",
                            location = session.location ?: "",
                            rating = session.rating ?: 0f,
                            notes = session.notes ?: "",
                            photos = session.photos,
                            isLoading = false,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Session not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load session: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateBrewingTime(duration: Duration) {
        val error = if (duration <= Duration.ZERO) {
            "Brewing time must be greater than 0"
        } else null

        _state.update {
            it.copy(
                brewingTime = duration,
                brewingTimeError = error
            )
        }
    }

    private fun updateTemperature(temperature: String) {
        val error = when {
            temperature.isBlank() -> "Temperature is required"
            temperature.toIntOrNull() == null -> "Invalid temperature"
            temperature.toInt() !in 0..100 -> "Temperature must be 0-100°C"
            else -> null
        }

        _state.update {
            it.copy(
                temperatureCelsius = temperature,
                temperatureError = error
            )
        }
    }

    private fun selectVessel(vessel: BrewingVessel) {
        _state.update {
            it.copy(
                selectedVessel = vessel,
                vesselError = null
            )
        }
    }

    private fun selectWaterType(waterType: WaterType) {
        _state.update { it.copy(selectedWaterType = waterType) }
    }

    private fun updateWaterQuantity(quantity: String) {
        val error = when {
            quantity.isBlank() -> "Water quantity is required"
            quantity.toIntOrNull() == null -> "Invalid quantity"
            quantity.toInt() <= 0 -> "Quantity must be greater than 0"
            else -> null
        }

        _state.update {
            it.copy(
                waterQuantityMl = quantity,
                waterQuantityError = error
            )
        }
    }

    private fun updateTeaQuantity(quantity: String) {
        val error = if (quantity.isNotBlank()) {
            when {
                quantity.toFloatOrNull() == null -> "Invalid quantity"
                quantity.toFloat() <= 0 -> "Quantity must be greater than 0"
                else -> null
            }
        } else null

        _state.update {
            it.copy(
                teaQuantityGrams = quantity,
                teaQuantityError = error
            )
        }
    }

    private fun updateLocation(location: String) {
        _state.update { it.copy(location = location) }
    }

    private fun updateRating(rating: Float) {
        val error = if (rating !in 0f..5f) {
            "Rating must be between 0 and 5"
        } else null

        _state.update {
            it.copy(
                rating = rating,
                ratingError = error
            )
        }
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

    private fun save() {
        val currentState = _state.value

        if (!currentState.isValid) {
            return
        }

        val session = currentState.existingSession ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val result = if (currentState.isParentSession) {
                // Update only parent-specific fields (not steep parameters)
                updateSessionUseCase(
                    existingSession = session,
                    vesselId = currentState.selectedVessel?.id,
                    waterType = currentState.selectedWaterType,
                    waterQuantityMl = currentState.waterQuantityMl.toIntOrNull(),
                    teaQuantityGrams = currentState.teaQuantityGrams.toFloatOrNull(),
                    location = currentState.location.takeIf { it.isNotBlank() },
                )
            } else {
                // Update only steep-specific fields
                updateSessionUseCase(
                    existingSession = session,
                    brewingTime = currentState.brewingTime,
                    temperatureCelsius = currentState.temperatureCelsius.toIntOrNull(),
                    rating = if (currentState.rating > 0f) currentState.rating else null,
                    notes = currentState.notes.takeIf { it.isNotBlank() },
                    photos = currentState.photos,
                )
            }

            result
                .onSuccess { updatedSession ->
                    _state.update { it.copy(isSaving = false, savedSession = updatedSession) }

                    // Check if we should show the save configuration dialog
                    // Only for steep edits with rating >= 5
                    if (!currentState.isParentSession &&
                        updatedSession.rating != null &&
                        updatedSession.rating >= 5f
                    ) {
                        _state.update { it.copy(showSaveConfigurationDialog = true) }
                    } else {
                        _navEvents.send(EditSessionNavEvent.NavigateBack)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = "Failed to save session: ${e.message}"
                        )
                    }
                }
        }
    }

    private fun handleBack() {
        if (_state.value.hasChanges) {
            _state.update { it.copy(showDiscardDialog = true) }
        } else {
            _navEvents.trySend(EditSessionNavEvent.NavigateBack)
        }
    }

    private fun confirmDiscard() {
        _state.update { it.copy(showDiscardDialog = false) }
        _navEvents.trySend(EditSessionNavEvent.NavigateBack)
    }

    private fun cancelDiscard() {
        _state.update { it.copy(showDiscardDialog = false) }
    }
}

sealed interface EditSessionNavEvent {
    data object NavigateBack : EditSessionNavEvent
}
