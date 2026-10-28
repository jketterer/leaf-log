package dev.jketterer.leaflog.presentation.ui.screens.log

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.dominantTags
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerFilter
import dev.jketterer.leaflog.presentation.ui.viewmodel.InProgressDialogState
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Duration

data class LogTeaState(
    // Tea selection
    val selectedTea: Tea? = null,
    val selectedTeaType: TeaType? = null,
    val availableTeas: List<Tea> = emptyList(),
    val suggestedTeas: List<Tea> = emptyList(),
    val teaPickerQuery: String = "",
    val showTeaPicker: Boolean = false,
    val teaPickerFilter: TeaPickerFilter = TeaPickerFilter.All,
    val availableTeaTypes: List<TeaType> = emptyList(),

    // The pair's last and best completed brews, which the source card switches between
    val lastBrew: SessionWithSteeps? = null,
    val bestBrew: SessionWithSteeps? = null,

    // The brew a session started here replays, and every tag, to name its dominant ones
    val sourceBrew: SessionWithSteeps? = null,
    val tagVocabulary: List<TastingTag> = emptyList(),

    // Brewing parameters
    val teaQuantityGrams: Double? = null,
    val isTeaBag: Boolean = false,
    val waterQuantityMl: Double? = null,
    val temperatureCelsius: Double? = null,
    val brewingTime: Duration? = null,
    val selectedVessel: BrewingVessel? = null,
    val selectedWaterType: WaterType = WaterType.FILTERED,
    val rinsed: Boolean = false,
    val location: String = "",

    // Optional details
    val photos: List<String> = emptyList(),

    // Available options
    val availableVessels: List<BrewingVessel> = emptyList(),

    // A default temperature the form started from, for the banner
    val prefillSource: PrefillSource = PrefillSource.None,

    // Validation errors
    val teaError: String? = null,
    val vesselError: String? = null,
    val teaQuantityError: StringResource? = null,
    val waterQuantityError: StringResource? = null,
    val temperatureError: StringResource? = null,
    val brewingTimeError: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,

    // User preferences
    val userPreferences: UserPreferences = UserPreferences(),

    // In-progress session conflict dialog
    val inProgressDialogState: InProgressDialogState? = null,
) {
    val isValid: Boolean
        get() = selectedTea != null &&
                selectedVessel != null &&
                teaError == null &&
                vesselError == null &&
                (isTeaBag || (teaQuantityGrams != null && teaQuantityError == null)) &&
                waterQuantityMl != null && waterQuantityError == null &&
                temperatureCelsius != null && temperatureError == null &&
                brewingTime != null && brewingTimeError == null

    val canSave: Boolean
        get() = isValid && !isSaving

    /** Derived, not loaded, so the tags land in the same frame as the brew they belong to. */
    val sourceBrewTags: List<TastingTag>
        get() = sourceBrew?.dominantTags(tagVocabulary).orEmpty()
}