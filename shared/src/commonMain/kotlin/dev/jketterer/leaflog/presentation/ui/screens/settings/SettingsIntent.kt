package dev.jketterer.leaflog.presentation.ui.screens.settings

import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.ThemeMode
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType

sealed interface SettingsIntent {
    data class UpdateThemeMode(val mode: ThemeMode) : SettingsIntent
    data class UpdateTemperatureUnit(val unit: TemperatureUnit) : SettingsIntent
    data class UpdateVolumeUnit(val unit: VolumeUnit) : SettingsIntent
    data class UpdateDefaultWaterType(val waterType: WaterType) : SettingsIntent
    data class UpdateTimerCompletionNotifications(val enabled: Boolean) : SettingsIntent
    data class UpdateSessionReminderNotifications(val enabled: Boolean) : SettingsIntent
    data object ExportData : SettingsIntent
    data object ExportPickerShown : SettingsIntent
    data object ExportCompleted : SettingsIntent
    data object ExportCancelled : SettingsIntent
    data object ExportFailed : SettingsIntent
    data object ImportData : SettingsIntent
    data object ImportPickerShown : SettingsIntent
    data class ImportFileSelected(val filePath: String) : SettingsIntent
    data object ImportCancelled : SettingsIntent
    data object ImportFailed : SettingsIntent
    data object ConfirmImport : SettingsIntent
    data object CancelImport : SettingsIntent
    data object DismissImportResult : SettingsIntent
}
