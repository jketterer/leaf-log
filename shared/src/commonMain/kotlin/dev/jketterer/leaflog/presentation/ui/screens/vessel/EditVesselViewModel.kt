package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.usecases.vessel.CreateBrewingVesselUseCase
import dev.jketterer.leaflog.domain.usecases.vessel.UpdateBrewingVesselUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_vessel
import leaf_log.shared.generated.resources.couldnt_save_photo
import leaf_log.shared.generated.resources.couldnt_save_vessel
import leaf_log.shared.generated.resources.failure_not_found
import kotlin.uuid.Uuid

class EditVesselViewModel(
    private val route: NavRoute.EditVesselRoute,
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

    val messages = UiMessages<Nothing>()

    init {
        loadVessel(route.vesselId)
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
    }

    fun onIntent(intent: EditVesselIntent) {
        when (intent) {
            is EditVesselIntent.NameChanged -> onNameChanged(intent.name)
            is EditVesselIntent.IconSelected -> onIconSelected(intent.iconName)
            is EditVesselIntent.CapacityChanged -> onCapacityChanged(intent.capacityMl)
            is EditVesselIntent.PhotoSelected -> onPhotoSelected(intent.imageBytes)
            is EditVesselIntent.RemovePhoto -> onRemovePhoto()
            is EditVesselIntent.SaveClicked -> saveVessel()
            is EditVesselIntent.Discard -> discard()
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
                    _state.update { it.copy(isLoading = false) }
                    messages.show(Res.string.failure_not_found)
                    return@launch
                }
                _state.update {
                    it.copy(
                        isEditMode = true,
                        existingVessel = vessel,
                        name = vessel.name,
                        selectedIconName = vessel.iconName ?: DEFAULT_ICON_NAME,
                        imagePath = vessel.imagePath,
                        capacityMl = vessel.capacityMl?.toDouble(),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_vessel)
            }
        }
    }

    private fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, nameError = validateVesselName(name)) }
    }

    private fun onIconSelected(iconName: String) {
        _state.update { it.copy(selectedIconName = iconName) }
    }

    private fun onCapacityChanged(capacityMl: Double?) {
        _state.update {
            it.copy(capacityMl = capacityMl, capacityError = validateCapacity(capacityMl))
        }
    }

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
                messages.failure(e, Res.string.couldnt_save_photo)
            }
        }
    }

    private fun onRemovePhoto() {
        _state.update { it.copy(imagePath = null) }
    }

    private fun saveVessel() {
        val currentState = _state.value

        val nameError = validateVesselName(currentState.name)
        val capacityError = validateCapacity(currentState.capacityMl)
        if (nameError != null || capacityError != null) {
            _state.update { it.copy(nameError = nameError, capacityError = capacityError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

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
                    capacityMl = currentState.storedCapacityMl,
                )
            } else {
                createBrewingVesselUseCase(
                    name = currentState.name,
                    iconName = currentState.selectedIconName,
                    imagePath = imagePath,
                    capacityMl = currentState.storedCapacityMl,
                )
            }

            result
                .onSuccess { _navEvents.send(EditVesselNavEvent.NavigateBack) }
                .onFailure { error ->
                    _state.update { it.copy(isSaving = false) }
                    messages.failure(error, Res.string.couldnt_save_vessel)
                }
        }
    }

    private fun discard() {
        // A photo is written to disk the moment it is picked, so abandoning the form has to take
        // the file with it or it is orphaned for good.
        val currentImagePath = _state.value.imagePath
        val existingImagePath = _state.value.existingVessel?.imagePath
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
}

sealed interface EditVesselNavEvent {
    data object NavigateBack : EditVesselNavEvent
}
