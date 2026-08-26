package dev.jketterer.leaflog.presentation.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.repositories.DataExportRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val dataExportRepository: DataExportRepository,
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
            is SettingsIntent.UpdateDefaultWaterType -> updateDefaultWaterType(intent.waterType)
            is SettingsIntent.ClearError -> clearError()
            is SettingsIntent.ExportData -> exportData()
            is SettingsIntent.ExportCompleted -> _state.update {
                it.copy(exportFilePath = null, isExporting = false)
            }
            is SettingsIntent.ImportData -> _state.update { it.copy(showImportPicker = true) }
            is SettingsIntent.ImportFileSelected -> importData(intent.filePath)
            is SettingsIntent.ImportCancelled -> _state.update { it.copy(showImportPicker = false) }
            is SettingsIntent.DismissImportResult -> _state.update { it.copy(importResult = null) }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesRepository.getPreferencesFlow()
                .catch { e -> Logger.w(tag = "Settings") { "Failed to load preferences: ${e.message}" } }
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

    private fun updateDefaultWaterType(waterType: dev.jketterer.leaflog.domain.models.WaterType) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultWaterType(waterType)
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to update default water type")
                    }
                }
        }
    }

    private fun exportData() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            dataExportRepository.exportAll()
                .onSuccess { filePath ->
                    _state.update { it.copy(exportFilePath = filePath, isExporting = false) }
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

    private fun importData(filePath: String) {
        viewModelScope.launch {
            _state.update { it.copy(showImportPicker = false, isImporting = true) }
            dataExportRepository.importFrom(filePath)
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
