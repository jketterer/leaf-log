package dev.jketterer.leaflog.presentation.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.usecases.preferences.GetPreferencesUseCase
import dev.jketterer.leaflog.domain.usecases.preferences.UpdateTemperatureUnitUseCase
import dev.jketterer.leaflog.domain.usecases.preferences.UpdateVolumeUnitUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getPreferencesUseCase: GetPreferencesUseCase,
    private val updateTemperatureUnitUseCase: UpdateTemperatureUnitUseCase,
    private val updateVolumeUnitUseCase: UpdateVolumeUnitUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadPreferences()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateTemperatureUnit -> updateTemperatureUnit(intent.unit)
            is SettingsIntent.UpdateVolumeUnit -> updateVolumeUnit(intent.unit)
            is SettingsIntent.ClearError -> clearError()
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            getPreferencesUseCase().collect { preferences ->
                _state.update { it.copy(preferences = preferences, isLoading = false) }
            }
        }
    }

    private fun updateTemperatureUnit(unit: dev.jketterer.leaflog.domain.models.TemperatureUnit) {
        viewModelScope.launch {
            updateTemperatureUnitUseCase(unit).onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to update temperature unit") }
            }
        }
    }

    private fun updateVolumeUnit(unit: dev.jketterer.leaflog.domain.models.VolumeUnit) {
        viewModelScope.launch {
            updateVolumeUnitUseCase(unit).onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to update volume unit") }
            }
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
