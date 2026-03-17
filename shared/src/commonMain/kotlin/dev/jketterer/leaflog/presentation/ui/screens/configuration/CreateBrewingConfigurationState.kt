package dev.jketterer.leaflog.presentation.ui.screens.configuration

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

data class CreateBrewingConfigurationState(
    val vessels: List<BrewingVessel> = emptyList(),
    val selectedVessel: BrewingVessel? = null,
    val label: String = "",
    val teaQuantityGrams: String = "",
    val waterQuantityMl: String = "",
    val waterQuantityDisplay: String = "",
    val temperatureCelsius: String = "",
    val temperatureDisplay: String = "",
    val brewingTime: Duration? = null,
    val waterType: WaterType = WaterType.SPRING,
    val userPreferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val editingConfigId: String? = null,
) {
    val canSave: Boolean
        get() = selectedVessel != null &&
                temperatureCelsius.toDoubleOrNull() != null &&
                waterQuantityMl.toDoubleOrNull() != null &&
                brewingTime != null
}
