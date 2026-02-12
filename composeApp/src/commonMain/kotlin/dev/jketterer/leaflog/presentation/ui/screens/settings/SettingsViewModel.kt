package dev.jketterer.leaflog.presentation.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.usecases.data.ExportDataUseCase
import dev.jketterer.leaflog.domain.usecases.data.ImportDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
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
            is SettingsIntent.ExportData -> exportData()
            is SettingsIntent.ExportCompleted -> _state.update {
                it.copy(exportJson = null, isExporting = false)
            }
            is SettingsIntent.ImportData -> _state.update { it.copy(showImportPicker = true) }
            is SettingsIntent.ImportFileSelected -> importData(intent.jsonContent)
            is SettingsIntent.ImportCancelled -> _state.update { it.copy(showImportPicker = false) }
            is SettingsIntent.DismissImportResult -> _state.update { it.copy(importResult = null) }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesRepository.getPreferencesFlow()
                .catch { e -> println("Failed to load preferences: ${e.message}") }
                .collect { preferences ->
                    _state.update { it.copy(preferences = preferences, isLoading = false) }
                }
        }
    }

    private fun updateTemperatureUnit(unit: dev.jketterer.leaflog.domain.models.TemperatureUnit) {
        viewModelScope.launch {
            preferencesRepository.updateTemperatureUnit(unit)
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to update temperature unit")
                    }
                }
        }
    }

    private fun updateVolumeUnit(unit: dev.jketterer.leaflog.domain.models.VolumeUnit) {
        viewModelScope.launch {
            preferencesRepository.updateVolumeUnit(unit)
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to update volume unit")
                    }
                }
        }
    }

    private fun exportData() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            exportDataUseCase()
                .onSuccess { json ->
                    _state.update { it.copy(exportJson = json, isExporting = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isExporting = false,
                            error = error.message ?: "Failed to export data",
                        )
                    }
                }
        }
    }

    private fun importData(jsonContent: String) {
        viewModelScope.launch {
            _state.update { it.copy(showImportPicker = false, isImporting = true) }
            importDataUseCase(jsonContent)
                .onSuccess { result ->
                    _state.update { it.copy(isImporting = false, importResult = result) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isImporting = false,
                            error = error.message ?: "Failed to import data",
                        )
                    }
                }
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
