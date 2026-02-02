package dev.jketterer.leaflog.presentation.ui.screens.settings

import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.VolumeUnit

sealed interface SettingsIntent {
    data class UpdateTemperatureUnit(val unit: TemperatureUnit) : SettingsIntent
    data class UpdateVolumeUnit(val unit: VolumeUnit) : SettingsIntent
    data object ClearError : SettingsIntent
}
