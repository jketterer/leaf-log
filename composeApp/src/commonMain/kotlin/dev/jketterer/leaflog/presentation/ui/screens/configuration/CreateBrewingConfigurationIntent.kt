package dev.jketterer.leaflog.presentation.ui.screens.configuration

import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

sealed interface CreateBrewingConfigurationIntent {
    data class LoadData(val teaId: String) : CreateBrewingConfigurationIntent
    data class SelectVessel(val vesselId: String) : CreateBrewingConfigurationIntent
    data class UpdateLabel(val label: String) : CreateBrewingConfigurationIntent
    data class UpdateTeaQuantity(val quantity: String) : CreateBrewingConfigurationIntent
    data class UpdateWaterQuantity(val quantity: String) : CreateBrewingConfigurationIntent
    data class UpdateTemperature(val temperature: String) : CreateBrewingConfigurationIntent
    data class UpdateBrewingTime(val duration: Duration) : CreateBrewingConfigurationIntent
    data class SelectWaterType(val waterType: WaterType) : CreateBrewingConfigurationIntent
    data object ToggleTemperatureUnit : CreateBrewingConfigurationIntent
    data object ToggleVolumeUnit : CreateBrewingConfigurationIntent
    data object Save : CreateBrewingConfigurationIntent
    data object NavigateBack : CreateBrewingConfigurationIntent
}
