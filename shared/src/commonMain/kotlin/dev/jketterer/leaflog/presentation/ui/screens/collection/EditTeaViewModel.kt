package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.EditTeaUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
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
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_tea
import leaf_log.shared.generated.resources.couldnt_save_photo
import leaf_log.shared.generated.resources.couldnt_save_tea
import leaf_log.shared.generated.resources.failure_not_found
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

    val messages = UiMessages<Nothing>()

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
            is EditTeaIntent.Discard -> discard()
        }
    }

    private fun loadTeaTypes() {
        viewModelScope.launch {
            teaTypeRepository.getAllFlow()
                .catchError()
                .collect { teaTypes ->
                    _state.update { it.copy(availableTeaTypes = teaTypes) }
                }

        }
    }

    private fun loadProducers() {
        viewModelScope.launch {
            teaRepository.getDistinctProducersFlow()
                .catchError()
                .collect { producers ->
                    _state.update { it.copy(availableProducers = producers) }
                }
        }
    }

    private fun <T> Flow<T>.catchError(): Flow<T> {
        return catch { e ->
            _state.update { it.copy(isLoading = false) }
            messages.failure(e, Res.string.couldnt_load_tea)
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
                .catchError()
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
                        _state.update { it.copy(isLoading = false) }
                        messages.show(Res.string.failure_not_found)
                    }
                }
        }
    }

    private fun updateName(name: String) {
        _state.update { it.copy(name = name, nameError = validateTeaName(name)) }
    }

    private fun updateTeaType(teaTypeId: String) {
        _state.update { state ->
            val seed = seedTemperatureForTeaType(
                currentCelsius = state.defaultTemperatureCelsius,
                currentFromTeaTypeId = state.temperatureFromTeaTypeId,
                newTeaTypeId = teaTypeId,
                newTypeDefaultCelsius = state.availableTeaTypes
                    .find { it.id == teaTypeId }
                    ?.defaultTemperatureCelsius,
            )
            val reseeded = seed.celsius != state.defaultTemperatureCelsius

            state.copy(
                selectedTeaTypeId = teaTypeId,
                teaTypeError = null,
                defaultTemperatureCelsius = seed.celsius,
                temperatureFromTeaTypeId = seed.fromTeaTypeId,
                // Cleared so the screen reconverts the new value into the user's display unit.
                defaultTemperatureDisplay = if (reseeded) "" else state.defaultTemperatureDisplay,
                temperatureError = if (reseeded) null else state.temperatureError,
            )
        }
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

        _state.update {
            it.copy(
                defaultTemperatureCelsius = storageValue,
                defaultTemperatureDisplay = temperature,
                // Typing makes the value theirs, so a later tea type change leaves it alone.
                temperatureFromTeaTypeId = null,
                // Validated in the display unit so the message quotes bounds the user recognises.
                temperatureError = validateTemperature(temperature, tempUnit),
            )
        }
    }

    private fun updateDescription(description: String) {
        _state.update { it.copy(description = description) }
    }

    private fun addPhoto(imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                val fileName = "${Uuid.random()}.jpg"
                val path = imageStorage.saveImage(imageBytes, fileName, "tea_images")
                newlyAddedPhotos.add(path)
                _state.update { it.copy(photos = it.photos + path) }
            } catch (e: Exception) {
                messages.failure(e, Res.string.couldnt_save_photo)
            }
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
                    nameError = validateTeaName(it.name),
                    teaTypeError = if (it.selectedTeaTypeId == null) "Tea type is required" else it.teaTypeError,
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
                    teaTypeId = currentState.selectedTeaTypeId!!,
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
                    _state.update { it.copy(isSaving = false) }
                    messages.failure(e, Res.string.couldnt_save_tea)
                }
        }
    }

    private fun discard() {
        viewModelScope.launch {
            newlyAddedPhotos.forEach { imageStorage.deleteImage(it) }
            newlyAddedPhotos.clear()
            _navEvents.trySend(EditTeaNavEvent.NavigateBack)
        }
    }
}

sealed interface EditTeaNavEvent {
    data object NavigateBack : EditTeaNavEvent
}