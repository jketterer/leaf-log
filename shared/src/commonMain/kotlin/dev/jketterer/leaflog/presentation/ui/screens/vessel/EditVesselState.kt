package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.UserPreferences
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.error_capacity_positive
import org.jetbrains.compose.resources.StringResource
import kotlin.math.roundToInt

data class EditVesselState(
    val isEditMode: Boolean = false,
    val existingVessel: BrewingVessel? = null,
    val name: String = "",
    val selectedIconName: String = DEFAULT_ICON_NAME,
    val imagePath: String? = null,
    val capacityMl: Double? = null,
    val userPreferences: UserPreferences = UserPreferences(),
    val nameError: String? = null,
    val capacityError: StringResource? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    /** The capacity as stored: whole milliliters. */
    val storedCapacityMl: Int?
        get() = capacityMl?.roundToInt()

    val hasChanges: Boolean
        get() = when {
            // Nothing to lose until the vessel we are editing has actually arrived. Without this,
            // backing out of a still-loading form prompts to discard changes that do not exist.
            isLoading -> false
            existingVessel != null -> name != existingVessel.name ||
                selectedIconName != (existingVessel.iconName ?: DEFAULT_ICON_NAME) ||
                imagePath != existingVessel.imagePath ||
                storedCapacityMl != existingVessel.capacityMl

            else -> name.isNotBlank() ||
                selectedIconName != DEFAULT_ICON_NAME ||
                imagePath != null ||
                capacityMl != null
        }

    val isValid: Boolean
        get() = nameError == null && capacityError == null && name.isNotBlank()
}

/** The icon a vessel gets when its owner never picks one. */
internal const val DEFAULT_ICON_NAME = "generic"

/** Error message for a vessel name, or null when it is acceptable. */
internal fun validateVesselName(name: String): String? = when {
    name.isBlank() -> "Name is required"
    name.length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or less"
    else -> null
}

private const val MAX_NAME_LENGTH = 50

/** Error for a capacity in milliliters, or null when it is acceptable. Capacity is optional. */
internal fun validateCapacity(capacityMl: Double?): StringResource? =
    if (capacityMl != null && capacityMl <= 0) Res.string.error_capacity_positive else null
