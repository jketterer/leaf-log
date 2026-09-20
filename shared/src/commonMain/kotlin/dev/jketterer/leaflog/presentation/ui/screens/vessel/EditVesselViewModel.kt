package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.usecases.vessel.CreateBrewingVesselUseCase
import dev.jketterer.leaflog.domain.usecases.vessel.UpdateBrewingVesselUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EditVesselViewModel(
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val imageStorage: ImageStorage,
    private val createBrewingVesselUseCase: CreateBrewingVesselUseCase,
    private val updateBrewingVesselUseCase: UpdateBrewingVesselUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditVesselState())
    val state: StateFlow<EditVesselState> = _state.asStateFlow()

    private val _navEvents = Channel<EditVesselNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            // The capacity field holds what the user typed in whatever unit was current, so a unit
            // change has to rewrite it. Doing it here rather than in the toggle handler covers the
            // first emission too, which can land after an edited vessel has already been seeded.
            updateState = { state, prefs ->
                state.copy(
                    userPreferences = prefs,
                    capacity = convertCapacity(
                        input = state.capacity,
                        from = state.userPreferences.volumeUnit,
                        to = prefs.volumeUnit,
                    ),
                )
            },
        )
    }

    fun onIntent(intent: EditVesselIntent) {
        when (intent) {
            is EditVesselIntent.LoadVessel -> loadVessel(intent.vesselId)
            is EditVesselIntent.NameChanged -> onNameChanged(intent.name)
            is EditVesselIntent.IconSelected -> onIconSelected(intent.iconName)
            is EditVesselIntent.CapacityChanged -> onCapacityChanged(intent.capacity)
            is EditVesselIntent.ToggleVolumeUnit -> toggleVolumeUnit()
            is EditVesselIntent.PhotoSelected -> onPhotoSelected(intent.imageBytes)
            is EditVesselIntent.RemovePhoto -> onRemovePhoto()
            is EditVesselIntent.SaveClicked -> saveVessel()
            is EditVesselIntent.BackClicked -> onBackClicked()
            is EditVesselIntent.ConfirmDiscard -> confirmDiscard()
            is EditVesselIntent.CancelDiscard -> cancelDiscard()
            is EditVesselIntent.ErrorShown -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadVessel(vesselId: String?) {
        if (vesselId == null) {
            _state.update { it.copy(isEditMode = false) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val vessel = brewingVesselRepository.getById(vesselId)
                if (vessel == null) {
                    _state.update { it.copy(isLoading = false, error = "Vessel not found") }
                    return@launch
                }
                _state.update {
                    it.copy(
                        isEditMode = true,
                        existingVessel = vessel,
                        name = vessel.name,
                        selectedIconName = vessel.iconName ?: DEFAULT_ICON_NAME,
                        imagePath = vessel.imagePath,
                        capacity = capacityFromMl(
                            capacityMl = vessel.capacityMl,
                            unit = it.userPreferences.volumeUnit,
                        ),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load vessel")
                }
            }
        }
    }

    private fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, nameError = validateVesselName(name)) }
    }

    private fun onIconSelected(iconName: String) {
        _state.update { it.copy(selectedIconName = iconName) }
    }

    private fun onCapacityChanged(capacity: String) {
        _state.update {
            it.copy(capacity = capacity, capacityError = validateCapacity(capacity))
        }
    }

    private fun toggleVolumeUnit() {
        viewModelScope.launch {
            preferencesRepository.updateVolumeUnit(_state.value.userPreferences.volumeUnit.toggle())
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun onPhotoSelected(imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                val fileName = "${Uuid.random()}.jpg"
                val persistedPath = imageStorage.saveImage(imageBytes, fileName)
                // Clean up previous unsaved pick if there was one
                val oldPath = _state.value.imagePath
                val existingPath = _state.value.existingVessel?.imagePath
                if (oldPath != null && oldPath != existingPath) {
                    try {
                        imageStorage.deleteImage(oldPath)
                    } catch (_: Exception) {
                    }
                }
                _state.update { it.copy(imagePath = persistedPath) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save image: ${e.message}") }
            }
        }
    }

    private fun onRemovePhoto() {
        _state.update { it.copy(imagePath = null) }
    }

    private fun saveVessel() {
        val currentState = _state.value

        val nameError = validateVesselName(currentState.name)
        val capacityError = validateCapacity(currentState.capacity)
        if (nameError != null || capacityError != null) {
            _state.update { it.copy(nameError = nameError, capacityError = capacityError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            // The image is already on disk - onPhotoSelected persists it as soon as it is picked.
            val existingImagePath = currentState.existingVessel?.imagePath
            val imagePath = currentState.imagePath
            if (existingImagePath != null && existingImagePath != imagePath) {
                try {
                    imageStorage.deleteImage(existingImagePath)
                } catch (_: Exception) {
                    // Best-effort cleanup
                }
            }

            val result = if (currentState.existingVessel != null) {
                updateBrewingVesselUseCase(
                    existingVessel = currentState.existingVessel,
                    name = currentState.name,
                    iconName = currentState.selectedIconName,
                    imagePath = imagePath,
                    capacityMl = currentState.capacityMl,
                )
            } else {
                createBrewingVesselUseCase(
                    name = currentState.name,
                    iconName = currentState.selectedIconName,
                    imagePath = imagePath,
                    capacityMl = currentState.capacityMl,
                )
            }

            result
                .onSuccess { _navEvents.send(EditVesselNavEvent.NavigateBack) }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save vessel",
                        )
                    }
                }
        }
    }

    private fun onBackClicked() {
        if (_state.value.hasChanges) {
            _state.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { _navEvents.send(EditVesselNavEvent.NavigateBack) }
        }
    }

    private fun confirmDiscard() {
        // A photo is written to disk the moment it is picked, so abandoning the form has to take
        // the file with it or it is orphaned for good.
        val currentImagePath = _state.value.imagePath
        val existingImagePath = _state.value.existingVessel?.imagePath
        _state.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch {
            if (currentImagePath != null && currentImagePath != existingImagePath) {
                try {
                    imageStorage.deleteImage(currentImagePath)
                } catch (_: Exception) {
                }
            }
            _navEvents.send(EditVesselNavEvent.NavigateBack)
        }
    }

    private fun cancelDiscard() {
        _state.update { it.copy(showDiscardDialog = false) }
    }
}

sealed interface EditVesselNavEvent {
    data object NavigateBack : EditVesselNavEvent
}
