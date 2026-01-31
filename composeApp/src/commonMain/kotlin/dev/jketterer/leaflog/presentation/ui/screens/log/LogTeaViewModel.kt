package dev.jketterer.leaflog.presentation.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.SearchTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.CreateSessionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

class LogTeaViewModel(
    private val teaRepository: TeaRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val searchTeasUseCase: SearchTeasUseCase,
    private val createSessionUseCase: CreateSessionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LogTeaState())
    val state: StateFlow<LogTeaState> = _state.asStateFlow()

    private val _navEvents = Channel<LogTeaNavigationEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    init {
        onIntent(LogTeaIntent.LoadData)
    }

    fun onIntent(intent: LogTeaIntent) {
        when (intent) {
            is LogTeaIntent.LoadData -> loadData()
            is LogTeaIntent.ShowTeaSearchDialog -> showTeaSearchDialog()
            is LogTeaIntent.HideTeaSearchDialog -> hideTeaSearchDialog()
            is LogTeaIntent.TeaSearchQueryChanged -> updateTeaSearchQuery(intent.query)
            is LogTeaIntent.TeaSelected -> selectTea(intent.teaId)
            is LogTeaIntent.QuickAddTeaClicked -> showQuickAddTeaDialog()
            is LogTeaIntent.QuickAddTeaSaved -> handleQuickAddTeaSaved(intent.tea)
            is LogTeaIntent.TeaQuantityChanged -> updateTeaQuantity(intent.quantity)
            is LogTeaIntent.WaterQuantityChanged -> updateWaterQuantity(intent.quantity)
            is LogTeaIntent.TemperatureChanged -> updateTemperature(intent.temperature)
            is LogTeaIntent.BrewingTimeChanged -> updateBrewingTime(intent.duration)
            is LogTeaIntent.VesselSelected -> selectVessel(intent.vessel)
            is LogTeaIntent.WaterTypeSelected -> selectWaterType(intent.waterType)
            is LogTeaIntent.LocationChanged -> updateLocation(intent.location)
            is LogTeaIntent.NotesChanged -> updateNotes(intent.notes)
            is LogTeaIntent.AddPhotoClicked -> {
                // Photo picker handled by UI
            }

            is LogTeaIntent.PhotoSelected -> addPhoto(intent.photoUri)
            is LogTeaIntent.PhotoRemoved -> removePhoto(intent.photoUri)
            is LogTeaIntent.SaveAsDraft -> saveSession(isDraft = true, startTimer = false)
            is LogTeaIntent.SaveAsCompleted -> saveSession(isDraft = false, startTimer = false)

            is LogTeaIntent.StartTimerClicked -> saveSession(isDraft = true, startTimer = true)
            is LogTeaIntent.BackClicked -> _navEvents.trySend(LogTeaNavigationEvent.NavigateBack)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                collectTeas()
                collectVessels()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load data: ${e.message}"
                    )
                }
            }
        }
    }

    private fun collectTeas() = viewModelScope.launch {
        teaRepository.getAllFlow()
            .catch { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load teas: ${e.message}",
                    )
                }
            }
            .collect { teas ->
                _state.update {
                    it.copy(
                        availableTeas = teas,
                        isLoading = false,
                    )
                }
            }
    }

    private fun collectVessels() = viewModelScope.launch {
        brewingVesselRepository.getAllFlow()
            .catch { e ->
                println("Failed to load vessels: ${e.message}")
            }
            .collect { vessels ->
                _state.update {
                    it.copy(
                        availableVessels = vessels,
                        selectedVessel = it.selectedVessel ?: vessels.firstOrNull()
                    )
                }
            }
    }

    private fun showTeaSearchDialog() {
        _state.update { it.copy(showTeaSearchDialog = true) }
    }

    private fun hideTeaSearchDialog() {
        _state.update {
            it.copy(
                showTeaSearchDialog = false,
                teaSearchQuery = ""
            )
        }
    }

    private fun updateTeaSearchQuery(query: String) {
        _state.update { it.copy(teaSearchQuery = query) }

        if (query.isBlank()) {
            loadData()
            return
        }

        viewModelScope.launch {
            try {
                // Use the use case - it has business logic (trim, validation)
                val results = searchTeasUseCase(query)
                _state.update { it.copy(availableTeas = results) }
            } catch (e: Exception) {
                println("Search failed: ${e.message}")
            }
        }
    }

    private fun selectTea(teaId: String?) {
        val tea = _state.value.availableTeas.firstOrNull { it.id == teaId }
        _state.update {
            it.copy(
                selectedTea = tea,
                teaError = null,
                showTeaSearchDialog = false,
                teaSearchQuery = "",
                hasUnsavedChanges = true,
            )
        }

        // Pre-fill with tea defaults
        tea?.let { prefillDefaults(it) }
    }

    private fun prefillDefaults(tea: Tea) {
        viewModelScope.launch {
            // Try to get last session for this tea
            val lastSession = teaRepository.getById(tea.id)?.lastBrewedAt?.let {
                teaSessionRepository.getByTeaId(tea.id, limit = 1).firstOrNull()
            }

            // Get tea type for fallback defaults
            val teaType = teaTypeRepository.getById(tea.teaTypeId)

            _state.update { currentState ->
                currentState.copy(
                    // Use last session values if available, otherwise tea defaults, then tea type defaults
                    waterQuantityMl = lastSession?.waterQuantityMl?.toString()
                        ?: tea.defaultQuantity?.toString()
                        ?: "",
                    temperatureCelsius = lastSession?.temperatureCelsius?.toString()
                        ?: tea.defaultTemperatureCelsius?.toString()
                        ?: teaType?.defaultTemperatureCelsius?.toString()
                        ?: "",
                    brewingTime = lastSession?.brewingTime
                        ?: tea.defaultBrewingTime
                        ?: teaType?.defaultBrewingTime,
                    teaQuantityGrams = lastSession?.teaQuantityGrams?.toString() ?: "",
                )
            }
        }
    }

    private fun showQuickAddTeaDialog() {
        _state.update { it.copy(showQuickAddTeaDialog = true) }
    }

    private fun handleQuickAddTeaSaved(tea: Tea) {
        _state.update { it.copy(showQuickAddTeaDialog = false) }
        selectTea(tea.id)
    }

    private fun updateTeaQuantity(quantity: String) {
        _state.update {
            it.copy(
                teaQuantityGrams = quantity,
                hasUnsavedChanges = true
            )
        }
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
                waterQuantityError = error,
                hasUnsavedChanges = true
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
                temperatureError = error,
                hasUnsavedChanges = true,
            )
        }
    }

    private fun updateBrewingTime(duration: Duration) {
        val error = if (duration <= Duration.ZERO) {
            "Brewing time must be greater than 0"
        } else null

        _state.update {
            it.copy(
                brewingTime = duration,
                brewingTimeError = error,
                hasUnsavedChanges = true,
            )
        }
    }

    private fun selectVessel(vessel: BrewingVessel) {
        _state.update { currentState ->
            // Auto-populate water quantity if vessel has capacity and water quantity is empty
            val shouldAutoPopulate = vessel.capacityMl != null && currentState.waterQuantityMl.isBlank()
            val newWaterQuantity = if (shouldAutoPopulate) {
                vessel.capacityMl.toString()
            } else {
                currentState.waterQuantityMl
            }

            currentState.copy(
                selectedVessel = vessel,
                waterQuantityMl = newWaterQuantity,
                waterQuantityError = if (shouldAutoPopulate) null else currentState.waterQuantityError,
                hasUnsavedChanges = true
            )
        }
    }

    private fun selectWaterType(waterType: WaterType) {
        _state.update { it.copy(selectedWaterType = waterType, hasUnsavedChanges = true) }
    }

    private fun updateLocation(location: String) {
        _state.update { it.copy(location = location, hasUnsavedChanges = true) }
    }

    private fun updateNotes(notes: String) {
        _state.update {
            it.copy(
                notes = notes,
                hasUnsavedChanges = true
            )
        }
    }

    private fun addPhoto(photoUri: String) {
        _state.update {
            it.copy(
                photos = it.photos + photoUri,
                hasUnsavedChanges = true
            )
        }
    }

    private fun removePhoto(photoUri: String) {
        _state.update {
            it.copy(
                photos = it.photos - photoUri,
                hasUnsavedChanges = true
            )
        }
    }

    private fun saveSession(isDraft: Boolean, startTimer: Boolean) {
        val currentState = _state.value

        if (!currentState.isValid) {
            _state.update {
                it.copy(
                    teaError = if (it.selectedTea == null) "Tea is required" else null,
                    waterQuantityError = if (it.waterQuantityMl.toIntOrNull() == null) "Water quantity is required" else it.waterQuantityError,
                    temperatureError = if (it.temperatureCelsius.toIntOrNull() == null) "Temperature is required" else it.temperatureError,
                    brewingTimeError = if (it.brewingTime == null) "Brewing time is required" else it.brewingTimeError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            // Use the use case - it has business logic (validation, ID generation, timestamps)
            createSessionUseCase(
                teaId = currentState.selectedTea!!.id,
                teaQuantityGrams = currentState.teaQuantityGrams.toFloatOrNull(),
                vesselId = currentState.selectedVessel!!.id,
                waterType = currentState.selectedWaterType,
                location = currentState.location.takeIf { it.isNotBlank() },
                brewingTime = currentState.brewingTime!!,
                temperatureCelsius = currentState.temperatureCelsius.toInt(),
                waterQuantityMl = currentState.waterQuantityMl.toInt(),
                notes = currentState.notes.takeIf { it.isNotBlank() },
                photos = currentState.photos,
                status = if (isDraft) SessionStatus.DRAFT else SessionStatus.COMPLETED,
            )
                .onSuccess {
                    _state.update { it.copy(isSaving = false, hasUnsavedChanges = false) }

                    if (startTimer) {
                        _navEvents.send(LogTeaNavigationEvent.NavigateToTimer(it.id))
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = "Failed to save session: ${e.message}",
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        println("clearing LogTeaViewModel")
        super.onCleared()
    }
}

sealed interface LogTeaNavigationEvent {
    data object NavigateBack : LogTeaNavigationEvent
    data class NavigateToTimer(val sessionId: String) : LogTeaNavigationEvent
}