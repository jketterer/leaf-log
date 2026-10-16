package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UnitConverter
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.DeleteTeaTypeUseCase
import dev.jketterer.leaflog.domain.usecases.MergeTeaTypeUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_delete_tea_type
import leaf_log.shared.generated.resources.couldnt_load_tea_type
import leaf_log.shared.generated.resources.couldnt_merge_tea_type
import leaf_log.shared.generated.resources.couldnt_save_tea_type
import leaf_log.shared.generated.resources.failure_not_found
import kotlin.time.Instant
import kotlin.uuid.Uuid

class EditTeaTypeViewModel(
    private val teaTypeRepository: TeaTypeRepository,
    private val teaRepository: TeaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val mergeTeaTypeUseCase: MergeTeaTypeUseCase,
    private val deleteTeaTypeUseCase: DeleteTeaTypeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditTeaTypeState())
    val state: StateFlow<EditTeaTypeState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

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
            is EditTeaTypeIntent.DeleteClicked -> onDeleteClicked()
            is EditTeaTypeIntent.ConfirmDelete -> confirmDelete()
            is EditTeaTypeIntent.CancelDelete -> _state.update {
                it.copy(showDeleteConfirmation = false, mergeTargetId = null)
            }

            is EditTeaTypeIntent.MergeTargetSelected -> _state.update {
                it.copy(mergeTargetId = intent.teaTypeId)
            }

            is EditTeaTypeIntent.ConfirmMerge -> confirmMerge()
            is EditTeaTypeIntent.ToggleTemperatureUnit -> toggleTemperatureUnit()
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
                        UnitConverter.celsiusToDisplayTemperature(it, prefs.temperatureUnit)
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
                    _state.update { it.copy(isLoading = false) }
                    messages.show(Res.string.failure_not_found)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_tea_type)
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

    private fun toggleTemperatureUnit() {
        viewModelScope.launch {
            preferencesRepository.updateTemperatureUnit(_state.value.userPreferences.temperatureUnit.toggle())
            _state.update { it.copy(temperature = "") }
        }
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
            _state.update { it.copy(isSaving = true) }

            try {
                val tempUnit = currentState.userPreferences.temperatureUnit
                val temperatureCelsius = currentState.temperature.toIntOrNull()?.let {
                    UnitConverter.inputTemperatureToCelsius(it, tempUnit)
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
                _state.update { it.copy(isSaving = false) }
                messages.failure(e, Res.string.couldnt_save_tea_type)
            }
        }
    }

    private fun onDeleteClicked() {
        val currentState = _state.value
        if (!currentState.isEditMode || currentState.existingTeaType == null) return

        viewModelScope.launch {
            val teaCount = teaRepository.getByTypeFlow(currentState.existingTeaType.id).first().size
            val mergeTargets = teaTypeRepository.getAll()
                .filter { it.id != currentState.existingTeaType.id }
            _state.update {
                it.copy(
                    showDeleteConfirmation = true,
                    canDelete = teaCount == 0,
                    teaCount = teaCount,
                    mergeTargets = mergeTargets,
                )
            }
        }
    }

    private fun confirmDelete() {
        val teaType = _state.value.existingTeaType ?: return

        viewModelScope.launch {
            deleteTeaTypeUseCase(teaType.id)
                .onSuccess { _navigationEvent.value = EditTeaTypeNavigationEvent.NavigateBack }
                .onFailure { e ->
                    _state.update { it.copy(showDeleteConfirmation = false) }
                    messages.failure(e, Res.string.couldnt_delete_tea_type)
                }
        }
    }

    private fun confirmMerge() {
        val teaType = _state.value.existingTeaType ?: return
        val targetId = _state.value.mergeTargetId ?: return

        viewModelScope.launch {
            mergeTeaTypeUseCase(sourceId = teaType.id, targetId = targetId)
                .onSuccess { _navigationEvent.value = EditTeaTypeNavigationEvent.NavigateBack }
                .onFailure { e ->
                    _state.update { it.copy(showDeleteConfirmation = false) }
                    messages.failure(e, Res.string.couldnt_merge_tea_type)
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
