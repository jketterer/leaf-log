package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import androidx.lifecycle.ViewModel
import kotlin.math.roundToInt
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UnitConverter
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EditTeaTypeViewModel(
    private val teaTypeRepository: TeaTypeRepository,
    private val teaRepository: TeaRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditTeaTypeState())
    val state: StateFlow<EditTeaTypeState> = _state.asStateFlow()

    private val _navigationEvent = MutableStateFlow<EditTeaTypeNavigationEvent?>(null)
    val navigationEvent: StateFlow<EditTeaTypeNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) },
        )
    }

    fun onIntent(intent: EditTeaTypeIntent) {
        when (intent) {
            is EditTeaTypeIntent.LoadTeaType -> loadTeaType(intent.teaTypeId)
            is EditTeaTypeIntent.NameChanged -> onNameChanged(intent.name)
            is EditTeaTypeIntent.ColorSelected -> onColorSelected(intent.colorHex)
            is EditTeaTypeIntent.TemperatureChanged -> onTemperatureChanged(intent.temperature)
            is EditTeaTypeIntent.SaveClicked -> save()
            is EditTeaTypeIntent.BackClicked -> onBackClicked()
            is EditTeaTypeIntent.ConfirmDiscard -> confirmDiscard()
            is EditTeaTypeIntent.CancelDiscard -> _state.update { it.copy(showDiscardDialog = false) }
            is EditTeaTypeIntent.DeleteClicked -> onDeleteClicked()
            is EditTeaTypeIntent.ConfirmDelete -> confirmDelete()
            is EditTeaTypeIntent.CancelDelete -> _state.update { it.copy(showDeleteConfirmation = false) }
        }
    }

    private fun loadTeaType(teaTypeId: String?) {
        if (teaTypeId == null) {
            _state.update { it.copy(isEditMode = false) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val teaType = teaTypeRepository.getById(teaTypeId)
                val prefs = preferencesRepository.getPreferences()
                if (teaType != null) {
                    val displayTemp = teaType.defaultTemperatureCelsius?.let {
                        UnitConverter.celsiusToDisplayTemperature(it.toDouble(), prefs.temperatureUnit)
                    }
                    val teaCount = teaRepository.getByTypeFlow(teaTypeId).first().size
                    _state.update {
                        it.copy(
                            isEditMode = true,
                            existingTeaType = teaType,
                            name = teaType.name,
                            colorHex = teaType.colorHex,
                            temperature = displayTemp?.toString() ?: "",
                            canDelete = teaCount == 0,
                            teaCount = teaCount,
                            isLoading = false,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(isLoading = false, error = "Tea type not found")
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load tea type")
                }
            }
        }
    }

    private fun onNameChanged(name: String) {
        _state.update {
            it.copy(name = name, nameError = validateName(name))
        }
    }

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Name is required"
            name.length > 50 -> "Name must be 50 characters or less"
            else -> null
        }
    }

    private fun onColorSelected(colorHex: String) {
        _state.update { it.copy(colorHex = colorHex) }
    }

    private fun onTemperatureChanged(temperature: String) {
        _state.update {
            it.copy(temperature = temperature, temperatureError = validateTemperature(temperature))
        }
    }

    private fun validateTemperature(temperature: String): String? {
        if (temperature.isBlank()) return null // Optional
        val value = temperature.toIntOrNull()
        return when {
            value == null -> "Must be a valid number"
            value <= 0 -> "Temperature must be greater than 0"
            value > 212 -> "Temperature seems too high"
            else -> null
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun save() {
        val currentState = _state.value
        val nameError = validateName(currentState.name)
        val temperatureError = validateTemperature(currentState.temperature)

        if (nameError != null || temperatureError != null || currentState.colorHex.isBlank()) {
            _state.update {
                it.copy(nameError = nameError, temperatureError = temperatureError)
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            try {
                val tempUnit = currentState.userPreferences.temperatureUnit
                val temperatureCelsius = currentState.temperature.toIntOrNull()?.let {
                    UnitConverter.inputTemperatureToCelsius(it, tempUnit).roundToInt()
                }

                val now = Instant.fromEpochMilliseconds(
                    kotlin.time.Clock.System.now().toEpochMilliseconds()
                )

                val teaType = if (currentState.isEditMode && currentState.existingTeaType != null) {
                    currentState.existingTeaType.copy(
                        name = currentState.name,
                        colorHex = currentState.colorHex,
                        defaultTemperatureCelsius = temperatureCelsius,
                        updatedAt = now,
                    )
                } else {
                    val allTeaTypes = teaTypeRepository.getAll()
                    val maxOrder = allTeaTypes.maxOfOrNull { it.displayOrder } ?: 0
                    TeaType(
                        id = Uuid.random().toString(),
                        name = currentState.name,
                        colorHex = currentState.colorHex,
                        defaultTemperatureCelsius = temperatureCelsius,
                        isSystemDefault = false,
                        displayOrder = maxOrder + 1,
                        createdAt = now,
                        updatedAt = now,
                    )
                }

                teaTypeRepository.upsert(teaType)
                _navigationEvent.value = EditTeaTypeNavigationEvent.NavigateBack
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, error = e.message ?: "Failed to save tea type")
                }
            }
        }
    }

    private fun onBackClicked() {
        if (_state.value.hasChanges) {
            _state.update { it.copy(showDiscardDialog = true) }
        } else {
            _navigationEvent.value = EditTeaTypeNavigationEvent.NavigateBack
        }
    }

    private fun confirmDiscard() {
        _state.update { it.copy(showDiscardDialog = false) }
        _navigationEvent.value = EditTeaTypeNavigationEvent.NavigateBack
    }

    private fun onDeleteClicked() {
        val currentState = _state.value
        if (!currentState.isEditMode || currentState.existingTeaType == null) return

        viewModelScope.launch {
            val teaCount = teaRepository.getByTypeFlow(currentState.existingTeaType.id).first().size
            _state.update {
                it.copy(
                    showDeleteConfirmation = true,
                    canDelete = teaCount == 0,
                    teaCount = teaCount,
                )
            }
        }
    }

    private fun confirmDelete() {
        val teaType = _state.value.existingTeaType ?: return

        viewModelScope.launch {
            try {
                teaTypeRepository.delete(teaType.id)
                _navigationEvent.value = EditTeaTypeNavigationEvent.NavigateBack
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        error = e.message ?: "Failed to delete tea type",
                    )
                }
            }
        }
    }

    fun onNavigationEventHandled() {
        _navigationEvent.value = null
    }
}

sealed interface EditTeaTypeNavigationEvent {
    data object NavigateBack : EditTeaTypeNavigationEvent
}
