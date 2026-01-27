package dev.jketterer.leaflog.presentation.ui.screens.log

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

data class LogTeaState(
    // Tea selection
    val selectedTea: Tea? = null,
    val availableTeas: List<Tea> = emptyList(),
    val teaSearchQuery: String = "",
    val showTeaSearchDialog: Boolean = false,

    // Brewing parameters
    val teaQuantityGrams: String = "",
    val waterQuantityMl: String = "",
    val temperatureCelsius: String = "",
    val brewingTime: Duration? = null,
    val selectedVessel: BrewingVessel? = null,
    val selectedWaterType: WaterType = WaterType.FILTERED,
    val location: String = "",

    // Optional details
    val notes: String = "",
    val photos: List<String> = emptyList(),

    // Available options
    val availableVessels: List<BrewingVessel> = emptyList(),

    // Validation errors
    val teaError: String? = null,
    val waterQuantityError: String? = null,
    val temperatureError: String? = null,
    val brewingTimeError: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showQuickAddTeaDialog: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
) {
    val isValid: Boolean
        get() = selectedTea != null &&
                waterQuantityMl.toIntOrNull() != null &&
                waterQuantityMl.toInt() > 0 &&
                temperatureCelsius.toIntOrNull() != null &&
                (temperatureCelsius.toInt() in 0..100) &&
                brewingTime != null &&
                brewingTime > Duration.ZERO &&
                selectedVessel != null

    val canSave: Boolean
        get() = isValid && !isSaving
}