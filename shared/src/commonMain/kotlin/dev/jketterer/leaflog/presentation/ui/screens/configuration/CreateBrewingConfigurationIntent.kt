package dev.jketterer.leaflog.presentation.ui.screens.configuration

import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

sealed interface CreateBrewingConfigurationIntent {
    data class LoadData(val teaId: String, val configurationId: String? = null) : CreateBrewingConfigurationIntent
    data class SelectVessel(val vesselId: String) : CreateBrewingConfigurationIntent
    data class UpdateLabel(val label: String) : CreateBrewingConfigurationIntent
    data class UpdateTeaQuantity(val grams: Double?) : CreateBrewingConfigurationIntent
    data class UpdateWaterQuantity(val milliliters: Double?) : CreateBrewingConfigurationIntent
    data class UpdateTemperature(val celsius: Double?) : CreateBrewingConfigurationIntent
    data class UpdateBrewingTime(val duration: Duration) : CreateBrewingConfigurationIntent
    data class SelectWaterType(val waterType: WaterType) : CreateBrewingConfigurationIntent
    data object Save : CreateBrewingConfigurationIntent
    data object NavigateBack : CreateBrewingConfigurationIntent
}
