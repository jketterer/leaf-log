package dev.jketterer.leaflog.presentation.ui.screens.log

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
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
    val waterQuantityDisplay: String = "", // User's input in display unit (preserves exact value)
    val temperatureCelsius: String = "",
    val temperatureDisplay: String = "", // User's input in display unit (preserves exact value)
    val brewingTime: Duration? = null,
    val selectedVessel: BrewingVessel? = null,
    val selectedWaterType: WaterType = WaterType.FILTERED,
    val location: String = "",

    // Optional details
    val notes: String = "",
    val photos: List<String> = emptyList(),

    // Available options
    val availableVessels: List<BrewingVessel> = emptyList(),

    // Pre-fill information
    val prefillSource: PrefillSource = PrefillSource.None,
    val usedConfigurationId: String? = null, // Track which configuration was used
    val availableConfigurations: List<BrewingConfiguration> = emptyList(),
    val showChooseMethodDialog: Boolean = false,
    val hasEditedBrewingParameters: Boolean = false, // Track if user has manually edited parameters

    // Validation errors
    val teaError: String? = null,
    val vesselError: String? = null,
    val waterQuantityError: String? = null,
    val temperatureError: String? = null,
    val brewingTimeError: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showQuickAddTeaDialog: Boolean = false,
    val hasUnsavedChanges: Boolean = false,

    // User preferences
    val userPreferences: UserPreferences = UserPreferences(),
) {
    val isValid: Boolean
        get() = selectedTea != null &&
                teaError == null &&
                vesselError == null &&
                waterQuantityError == null &&
                temperatureError == null &&
                brewingTimeError == null

    val canSave: Boolean
        get() = isValid && !isSaving
}