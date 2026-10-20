package dev.jketterer.leaflog.presentation.ui.screens.configuration

import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

data class CreateBrewingConfigurationState(
    val vessels: List<BrewingVessel> = emptyList(),
    val selectedVessel: BrewingVessel? = null,
    val label: String = "",
    val teaQuantityGrams: Double? = null,
    val waterQuantityMl: Double? = null,
    val temperatureCelsius: Double? = null,
    val brewingTime: Duration? = null,
    val steepIncrement: Duration? = null,
    val steepOverrides: Map<Int, Duration> = emptyMap(),
    val waterType: WaterType = WaterType.SPRING,
    val userPreferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val editingConfigId: String? = null,
) {
    val plan: BrewingPlan? get() = brewingTime?.let { BrewingPlan(it, steepIncrement, steepOverrides) }

    val canSave: Boolean
        get() = selectedVessel != null &&
                temperatureCelsius != null &&
                waterQuantityMl != null &&
                brewingTime != null
}
