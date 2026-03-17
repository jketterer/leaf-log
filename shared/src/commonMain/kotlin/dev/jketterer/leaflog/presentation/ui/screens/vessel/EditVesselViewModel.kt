package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.UnitConverter
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.usecases.vessel.CreateBrewingVesselUseCase
import dev.jketterer.leaflog.domain.usecases.vessel.UpdateBrewingVesselUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _navigationEvent = MutableStateFlow<EditVesselNavigationEvent?>(null)
    val navigationEvent: StateFlow<EditVesselNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
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
        }
    }

    private fun loadVessel(vesselId: String?) {
        if (vesselId == null) {
            // Add mode - set default icon
            _state.update {
                it.copy(
                    isEditMode = false,
                    selectedIconName = "generic"
                )
            }
            return
        }

        // Edit mode
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val vessel = brewingVesselRepository.getById(vesselId)
                // Capacity is already in mL (storage unit), store it directly
                if (vessel != null) {
                    _state.update {
                        it.copy(
                            isEditMode = true,
                            existingVessel = vessel,
                            name = vessel.name,
                            selectedIconName = vessel.iconName ?: "generic",
                            imagePath = vessel.imagePath,
                            capacity = vessel.capacityMl?.toString() ?: "",
                            isLoading = false,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Vessel not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load vessel"
                    )
                }
            }
        }
    }

    private fun onNameChanged(name: String) {
        _state.update {
            it.copy(
                name = name,
                nameError = validateName(name)
            )
        }
    }

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Name is required"
            name.length > 50 -> "Name must be 50 characters or less"
            else -> null
        }
    }

    private fun onIconSelected(iconName: String) {
        _state.update { it.copy(selectedIconName = iconName) }
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

    private fun onCapacityChanged(capacity: String) {
        // Convert from display unit to storage unit (mL)
        val storageValue = capacity.toIntOrNull()?.let { displayValue ->
            _state.value.userPreferences.volumeUnit.toMilliliters(displayValue).toInt().toString()
        } ?: capacity

        _state.update {
            it.copy(
                capacity = storageValue,
                capacityError = validateCapacity(capacity),
            )
        }
    }

    private fun validateCapacity(capacity: String): String? {
        if (capacity.isBlank()) {
            return null // Capacity is optional
        }
        val value = capacity.toIntOrNull()
        return when {
            value == null -> "Must be a valid number"
            value <= 0 -> "Capacity must be greater than 0"
            else -> null
        }
    }

    private fun toggleVolumeUnit() {
        viewModelScope.launch {
            preferencesRepository.updateVolumeUnit(_state.value.userPreferences.volumeUnit.toggle())
        }
    }

    private fun saveVessel() {
        val currentState = _state.value

        // Validate
        val nameError = validateName(currentState.name)
        val capacityError = validateCapacity(currentState.capacity)

        if (nameError != null || capacityError != null || currentState.selectedIconName == null) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    capacityError = capacityError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            // Capacity is already in mL (storage unit), use directly
            val capacityValue = currentState.capacity.toIntOrNull()

            // Image is already persisted to internal storage by onPhotoSelected
            val existingImagePath = currentState.existingVessel?.imagePath
            val imagePath = currentState.imagePath

            // Delete old image file if it was replaced or removed
            if (existingImagePath != null && existingImagePath != imagePath) {
                try {
                    imageStorage.deleteImage(existingImagePath)
                } catch (_: Exception) {
                    // Best-effort cleanup
                }
            }

            val result = if (currentState.isEditMode && currentState.existingVessel != null) {
                updateBrewingVesselUseCase(
                    existingVessel = currentState.existingVessel,
                    name = currentState.name,
                    iconName = currentState.selectedIconName,
                    imagePath = imagePath,
                    clearImage = imagePath == null && existingImagePath != null,
                    capacityMl = capacityValue,
                )
            } else {
                createBrewingVesselUseCase(
                    name = currentState.name,
                    iconName = currentState.selectedIconName,
                    imagePath = imagePath,
                    capacityMl = capacityValue,
                )
            }

            result
                .onSuccess {
                    _navigationEvent.value = EditVesselNavigationEvent.NavigateBackAfterSave
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save vessel"
                        )
                    }
                }
        }
    }

    private fun onBackClicked() {
        if (_state.value.hasChanges) {
            _state.update { it.copy(showDiscardDialog = true) }
        } else {
            _navigationEvent.value = EditVesselNavigationEvent.NavigateBack
        }
    }

    private fun confirmDiscard() {
        // Clean up any newly saved image that won't be used
        val currentImagePath = _state.value.imagePath
        val existingImagePath = _state.value.existingVessel?.imagePath
        if (currentImagePath != null && currentImagePath != existingImagePath) {
            viewModelScope.launch {
                try {
                    imageStorage.deleteImage(currentImagePath)
                } catch (_: Exception) {
                }
            }
        }
        _state.update { it.copy(showDiscardDialog = false) }
        _navigationEvent.value = EditVesselNavigationEvent.NavigateBack
    }

    private fun cancelDiscard() {
        _state.update { it.copy(showDiscardDialog = false) }
    }

    fun onNavigationEventHandled() {
        _navigationEvent.value = null
    }
}

sealed interface EditVesselNavigationEvent {
    data object NavigateBack : EditVesselNavigationEvent
    data object NavigateBackAfterSave : EditVesselNavigationEvent
}
