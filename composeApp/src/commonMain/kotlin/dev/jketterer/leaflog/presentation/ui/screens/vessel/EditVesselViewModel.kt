package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.UnitConverter
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.usecases.vessel.CreateBrewingVesselUseCase
import dev.jketterer.leaflog.domain.usecases.vessel.UpdateBrewingVesselUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditVesselViewModel(
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
    private val createBrewingVesselUseCase: CreateBrewingVesselUseCase,
    private val updateBrewingVesselUseCase: UpdateBrewingVesselUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditVesselState())
    val state: StateFlow<EditVesselState> = _state.asStateFlow()

    private val _navigationEvent = MutableStateFlow<EditVesselNavigationEvent?>(null)
    val navigationEvent: StateFlow<EditVesselNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadPreferences()
    }

    fun onIntent(intent: EditVesselIntent) {
        when (intent) {
            is EditVesselIntent.LoadVessel -> loadVessel(intent.vesselId)
            is EditVesselIntent.NameChanged -> onNameChanged(intent.name)
            is EditVesselIntent.IconSelected -> onIconSelected(intent.iconName)
            is EditVesselIntent.CapacityChanged -> onCapacityChanged(intent.capacity)
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
                val prefs = preferencesRepository.getPreferences()
                val vessel = brewingVesselRepository.getById(vesselId)
                val capacity = vessel?.capacityMl?.let {
                    UnitConverter.millilitersToDisplayVolume(it, prefs.volumeUnit)
                }
                if (vessel != null) {
                    _state.update {
                        it.copy(
                            isEditMode = true,
                            existingVessel = vessel,
                            name = vessel.name,
                            selectedIconName = vessel.iconName ?: "generic",
                            capacity = capacity?.toString() ?: "",
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

    private fun loadPreferences() = viewModelScope.launch {
        preferencesRepository.getPreferencesFlow().collect { prefs ->
            _state.update { it.copy(userPreferences = prefs) }
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

    private fun onCapacityChanged(capacity: String) {
        _state.update {
            it.copy(
                capacity = capacity,
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

            val volumeUnit = state.value.userPreferences.volumeUnit
            val capacityValue = currentState.capacity.toIntOrNull()?.let {
                UnitConverter.inputVolumeToMilliliters(it, volumeUnit)
            }

            val result = if (currentState.isEditMode && currentState.existingVessel != null) {
                // Update existing vessel
                updateBrewingVesselUseCase(
                    existingVessel = currentState.existingVessel,
                    name = currentState.name,
                    iconName = currentState.selectedIconName,
                    capacityMl = capacityValue,
                )
            } else {
                // Create new vessel
                createBrewingVesselUseCase(
                    name = currentState.name,
                    iconName = currentState.selectedIconName,
                    capacityMl = capacityValue
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
