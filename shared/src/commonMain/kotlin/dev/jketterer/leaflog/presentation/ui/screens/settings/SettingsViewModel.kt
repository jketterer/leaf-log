package dev.jketterer.leaflog.presentation.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.models.ThemeMode
import dev.jketterer.leaflog.domain.repositories.BackupRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.backup_exported
import leaf_log.shared.generated.resources.couldnt_export
import leaf_log.shared.generated.resources.couldnt_import
import leaf_log.shared.generated.resources.couldnt_open_backup
import leaf_log.shared.generated.resources.couldnt_read_backup
import leaf_log.shared.generated.resources.couldnt_save_backup
import leaf_log.shared.generated.resources.couldnt_save_setting
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Instant

private const val BACKUP_OVERDUE_AFTER_DAYS = 14

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

    init {
        loadPreferences()
        measurePhotoStorage()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateThemeMode -> updateThemeMode(intent.mode)
            is SettingsIntent.UpdateTemperatureUnit -> updateTemperatureUnit(intent.unit)
            is SettingsIntent.UpdateVolumeUnit -> updateVolumeUnit(intent.unit)
            is SettingsIntent.UpdateDefaultWaterType -> updateDefaultWaterType(intent.waterType)

            is SettingsIntent.UpdateTimerCompletionNotifications ->
                updateTimerCompletionNotifications(intent.enabled)

            is SettingsIntent.UpdateSessionReminderNotifications ->
                updateSessionReminderNotifications(intent.enabled)

            is SettingsIntent.ExportData -> exportData()
            is SettingsIntent.ExportPickerShown ->
                _state.update { it.copy(showExportPicker = false) }
            is SettingsIntent.ExportCompleted -> {
                recordBackup()
                finishExport()
                messages.show(Res.string.backup_exported)
            }
            is SettingsIntent.ExportCancelled -> finishExport()
            is SettingsIntent.ExportFailed -> {
                finishExport()
                messages.show(Res.string.couldnt_save_backup)
            }
            is SettingsIntent.ImportData -> _state.update { it.copy(showImportPicker = true) }
            is SettingsIntent.ImportPickerShown ->
                _state.update { it.copy(showImportPicker = false) }
            is SettingsIntent.ImportFileSelected -> previewImport(intent.filePath)
            is SettingsIntent.ImportCancelled -> _state.update { it.copy(showImportPicker = false) }
            is SettingsIntent.ImportFailed -> {
                _state.update { it.copy(showImportPicker = false) }
                messages.show(Res.string.couldnt_open_backup)
            }
            is SettingsIntent.ConfirmImport -> importData()
            is SettingsIntent.CancelImport -> cancelImport()
            is SettingsIntent.DismissImportResult -> _state.update { it.copy(importResult = null) }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesRepository.getPreferencesFlow()
                .catch { e -> Logger.w(tag = "Settings") { "Failed to load preferences: ${e.message}" } }
                .collect { preferences ->
                    val days = daysSinceBackup(
                        preferences.lastBackupAt,
                        Clock.System.now(),
                        TimeZone.currentSystemDefault(),
                    )
                    _state.update {
                        it.copy(
                            preferences = preferences,
                            isLoading = false,
                            daysSinceBackup = days,
                            isBackupOverdue = days == null || days > BACKUP_OVERDUE_AFTER_DAYS,
                        )
                    }
                }
        }
    }

    private fun measurePhotoStorage() {
        viewModelScope.launch {
            backupRepository.photoStorageBytes()
                .onSuccess { bytes -> _state.update { it.copy(photoStorage = formatStorageSize(bytes)) } }
                .onFailure { e ->
                    Logger.w(tag = "Settings") { "Failed to measure photo storage: ${e.message}" }
                }
        }
    }

    private fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
                .onFailure { messages.failure(it, Res.string.couldnt_save_setting) }
        }
    }

    private fun updateTemperatureUnit(unit: dev.jketterer.leaflog.domain.models.TemperatureUnit) {
        viewModelScope.launch {
            preferencesRepository.updateTemperatureUnit(unit)
                .onFailure { messages.failure(it, Res.string.couldnt_save_setting) }
        }
    }

    private fun updateVolumeUnit(unit: dev.jketterer.leaflog.domain.models.VolumeUnit) {
        viewModelScope.launch {
            preferencesRepository.updateVolumeUnit(unit)
                .onFailure { messages.failure(it, Res.string.couldnt_save_setting) }
        }
    }

    private fun updateDefaultWaterType(waterType: dev.jketterer.leaflog.domain.models.WaterType) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultWaterType(waterType)
                .onFailure { messages.failure(it, Res.string.couldnt_save_setting) }
        }
    }

    private fun updateTimerCompletionNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateTimerCompletionNotificationsEnabled(enabled)
                .onFailure { messages.failure(it, Res.string.couldnt_save_setting) }
        }
    }

    private fun updateSessionReminderNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateSessionReminderNotificationsEnabled(enabled)
                .onFailure { messages.failure(it, Res.string.couldnt_save_setting) }
        }
    }

    private fun exportData() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            backupRepository.export()
                .onSuccess { filePath ->
                    // Still exporting until the file is delivered
                    _state.update { it.copy(exportFilePath = filePath, showExportPicker = true) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isExporting = false) }
                    messages.failure(error, Res.string.couldnt_export)
                }
        }
    }

    private fun recordBackup() {
        viewModelScope.launch {
            preferencesRepository.updateLastBackupAt(Clock.System.now())
                .onFailure { e ->
                    Logger.w(tag = "Settings") { "Failed to record the backup: ${e.message}" }
                }
        }
    }

    private fun finishExport() {
        val path = _state.value.exportFilePath
        _state.update {
            it.copy(exportFilePath = null, showExportPicker = false, isExporting = false)
        }
        if (path != null) viewModelScope.launch { backupRepository.discard(path) }
    }

    private fun previewImport(filePath: String) {
        viewModelScope.launch {
            _state.update { it.copy(showImportPicker = false, isImporting = true) }
            backupRepository.preview(filePath)
                .onSuccess { summary ->
                    _state.update {
                        it.copy(isImporting = false, importPreview = ImportPreview(filePath, summary))
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isImporting = false) }
                    messages.failure(error, Res.string.couldnt_read_backup)
                }
        }
    }

    private fun importData() {
        val preview = _state.value.importPreview ?: return
        viewModelScope.launch {
            _state.update { it.copy(importPreview = null, isImporting = true) }
            backupRepository.import(preview.filePath)
                .onSuccess { result ->
                    _state.update { it.copy(isImporting = false, importResult = result) }
                    measurePhotoStorage()
                }
                .onFailure { error ->
                    _state.update { it.copy(isImporting = false) }
                    messages.failure(error, Res.string.couldnt_import)
                }
        }
    }

    private fun cancelImport() {
        val preview = _state.value.importPreview ?: return
        _state.update { it.copy(importPreview = null) }
        viewModelScope.launch { backupRepository.discard(preview.filePath) }
    }
}

/** Calendar days in [tz] from [lastBackupAt] to [now], or null if there's never been a backup. */
internal fun daysSinceBackup(lastBackupAt: Instant?, now: Instant, tz: TimeZone): Int? =
    lastBackupAt?.let {
        // A clock set back since the backup reads as today, not as a backup from the future
        it.toLocalDateTime(tz).date.daysUntil(now.toLocalDateTime(tz).date).coerceAtLeast(0)
    }

/**
 * [bytes] in decimal units, as both platforms' storage settings count them. Each unit stops where
 * rounding would reach 1000 of it.
 */
internal fun formatStorageSize(bytes: Long): String = when {
    bytes <= 999_000 -> "${(bytes + 999) / 1_000} KB"
    bytes < 999_500_000 -> "${(bytes + 500_000) / 1_000_000} MB"
    else -> "${(bytes / 100_000_000.0).roundToLong() / 10.0} GB"
}
