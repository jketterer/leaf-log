package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.EditTeaUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EditTeaViewModel(
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val preferencesRepository: PreferencesRepository,
    private val createTeaUseCase: CreateTeaUseCase,
    private val editTeaUseCase: EditTeaUseCase,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(EditTeaState())
    val state: StateFlow<EditTeaState> = _state.asStateFlow()

    private val _navEvents = Channel<EditTeaNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    private val newlyAddedPhotos = mutableListOf<String>()

    init {
        loadTeaTypes()
        loadProducers()
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) }
        )
    }

    fun onIntent(intent: EditTeaIntent) {
        when (intent) {
            is EditTeaIntent.LoadTea -> loadTea(intent.teaId)
            is EditTeaIntent.NameChanged -> updateName(intent.name)
            is EditTeaIntent.TeaTypeSelected -> updateTeaType(intent.teaTypeId)
            is EditTeaIntent.OriginChanged -> updateOrigin(intent.origin)
            is EditTeaIntent.ProducerChanged -> updateProducer(intent.producer)
            is EditTeaIntent.PurchaseDateChanged -> updatePurchaseDate(intent.date)
            is EditTeaIntent.TemperatureChanged -> updateTemperature(intent.temperature)
            is EditTeaIntent.DescriptionChanged -> updateDescription(intent.description)
            is EditTeaIntent.PhotoSelected -> addPhoto(intent.imageBytes)
            is EditTeaIntent.PhotoRemoved -> removePhoto(intent.photoPath)
            is EditTeaIntent.ToggleTemperatureUnit -> toggleTemperatureUnit()
            is EditTeaIntent.SaveClicked -> save()
            is EditTeaIntent.BackClicked -> handleBack()
            is EditTeaIntent.ConfirmDiscard -> confirmDiscard()
            is EditTeaIntent.CancelDiscard -> cancelDiscard()
        }
    }

    private fun loadTeaTypes() {
        viewModelScope.launch {
            teaTypeRepository.getAllFlow()
                .catchError("Failed to load tea types")
                .collect { teaTypes ->
                    _state.update { it.copy(availableTeaTypes = teaTypes) }
                }

        }
    }

    private fun loadProducers() {
        viewModelScope.launch {
            teaRepository.getDistinctProducersFlow()
                .catchError("Failed to load producers")
                .collect { producers ->
                    _state.update { it.copy(availableProducers = producers) }
                }
        }
    }

    private fun <T> Flow<T>.catchError(message: String): Flow<T> {
        return catch { e ->
            _state.update { it.copy(isLoading = false, error = "$message: ${e.message}") }
        }
    }

    private fun loadTea(teaId: String?) {
        if (teaId == null) {
            _state.update { it.copy(isEditMode = false) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isEditMode = true) }

            teaRepository.getByIdFlow(teaId)
                .catchError("Failed to load tea")
                .first()
                .let { tea ->
                    if (tea != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                existingTea = tea,
                                name = tea.name,
                                selectedTeaTypeId = tea.teaTypeId,
                                origin = tea.origin ?: "",
                                producer = tea.producer ?: "",
                                purchaseDate = tea.purchaseDate,
                                defaultTemperatureCelsius = tea.defaultTemperatureCelsius?.toString()
                                    ?: "",
                                description = tea.description ?: "",
                                photos = tea.photos,
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Tea not found") }
                    }
                }
        }
    }

    private fun updateName(name: String) {
        _state.update {
            it.copy(
                name = name,
                nameError = if (name.isBlank()) "Name is required" else null
            )
        }
    }

    private fun updateTeaType(teaTypeId: String) {
        _state.update { it.copy(selectedTeaTypeId = teaTypeId, teaTypeError = null) }
    }

    private fun updateOrigin(origin: String) {
        _state.update { it.copy(origin = origin) }
    }

    private fun updateProducer(producer: String) {
        _state.update { it.copy(producer = producer) }
    }

    private fun updatePurchaseDate(date: LocalDate?) {
        _state.update { it.copy(purchaseDate = date) }
    }

    private fun updateTemperature(temperature: String) {
        val currentState = _state.value
        val tempUnit = currentState.userPreferences.temperatureUnit

        // Convert from display unit to storage unit (Celsius)
        val storageValue = temperature.toIntOrNull()?.let { displayValue ->
            tempUnit.toCelsius(displayValue).toString()
        } ?: temperature

        // Validate in display unit for user-friendly error messages
        val error = when {
            temperature.isBlank() -> "Temperature is required"
            temperature.toIntOrNull() == null -> "Invalid temperature"
            else -> {
                val value = temperature.toInt()
                when (tempUnit) {
                    TemperatureUnit.CELSIUS -> {
                        if (value !in 0..100) "Temperature must be 0-100°C" else null
                    }

                    TemperatureUnit.FAHRENHEIT -> {
                        if (value !in 32..212) "Temperature must be 32-212°F" else null
                    }
                }
            }
        }

        _state.update {
            it.copy(
                defaultTemperatureCelsius = storageValue,
                defaultTemperatureDisplay = temperature,
                temperatureError = error,
            )
        }
    }

    private fun updateDescription(description: String) {
        _state.update { it.copy(description = description) }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun addPhoto(imageBytes: ByteArray) {
        viewModelScope.launch {
            val fileName = "${Uuid.random()}.jpg"
            val path = imageStorage.saveImage(imageBytes, fileName, "tea_images")
            newlyAddedPhotos.add(path)
            _state.update { it.copy(photos = it.photos + path) }
        }
    }

    private fun removePhoto(photoPath: String) {
        viewModelScope.launch {
            if (photoPath in newlyAddedPhotos) {
                imageStorage.deleteImage(photoPath)
                newlyAddedPhotos.remove(photoPath)
            }
            _state.update { it.copy(photos = it.photos - photoPath) }
        }
    }

    private fun toggleTemperatureUnit() {
        viewModelScope.launch {
            preferencesRepository.updateTemperatureUnit(_state.value.userPreferences.temperatureUnit.toggle())
            _state.update { it.copy(defaultTemperatureDisplay = "") } // Clear so screen reconverts from storage
        }
    }

    private fun save() {
        val currentState = _state.value

        if (!currentState.isValid) {
            _state.update {
                it.copy(
                    nameError = if (it.name.isBlank()) "Name is required" else it.nameError,
                    teaTypeError = if (it.selectedTeaTypeId == null) "Tea type is required" else it.teaTypeError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val result = if (currentState.isEditMode && currentState.existingTea != null) {
                val removedPhotos = currentState.existingTea.photos - currentState.photos.toSet()
                val editResult = editTeaUseCase(
                    existingTea = currentState.existingTea,
                    name = currentState.name,
                    teaTypeId = currentState.selectedTeaTypeId,
                    origin = currentState.origin.takeIf { it.isNotBlank() },
                    producer = currentState.producer.takeIf { it.isNotBlank() },
                    purchaseDate = currentState.purchaseDate,
                    defaultTemperatureCelsius = currentState.defaultTemperatureCelsius.toDoubleOrNull(),
                    description = currentState.description.takeIf { it.isNotBlank() },
                    photos = currentState.photos,
                )
                editResult.onSuccess {
                    removedPhotos.forEach { imageStorage.deleteImage(it) }
                }
                editResult
            } else {
                createTeaUseCase(
                    name = currentState.name,
                    teaTypeId = currentState.selectedTeaTypeId!!,
                    origin = currentState.origin.takeIf { it.isNotBlank() },
                    producer = currentState.producer.takeIf { it.isNotBlank() },
                    purchaseDate = currentState.purchaseDate,
                    defaultTemperatureCelsius = currentState.defaultTemperatureCelsius.toDoubleOrNull(),
                    description = currentState.description.takeIf { it.isNotBlank() },
                    photos = currentState.photos
                )
            }

            result.onSuccess {
                newlyAddedPhotos.clear()
                _state.update { it.copy(isSaving = false) }
                _navEvents.trySend(EditTeaNavEvent.NavigateBack)
            }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = "Failed to save tea: ${e.message}"
                        )
                    }
                }
        }
    }

    private fun handleBack() {
        if (_state.value.hasChanges) {
            _state.update { it.copy(showDiscardDialog = true) }
        } else {
            _navEvents.trySend(EditTeaNavEvent.NavigateBack)
        }
    }

    private fun confirmDiscard() {
        viewModelScope.launch {
            newlyAddedPhotos.forEach { imageStorage.deleteImage(it) }
            newlyAddedPhotos.clear()
            _navEvents.trySend(EditTeaNavEvent.NavigateBack)
        }
    }

    private fun cancelDiscard() {
        _state.update { it.copy(showDiscardDialog = false) }
    }
}

sealed interface EditTeaNavEvent {
    data object NavigateBack : EditTeaNavEvent
}