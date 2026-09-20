package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import kotlin.math.roundToInt

data class EditVesselState(
    val isEditMode: Boolean = false,
    val existingVessel: BrewingVessel? = null,
    val name: String = "",
    val selectedIconName: String = DEFAULT_ICON_NAME,
    val imagePath: String? = null,
    /** As the user typed it, in [userPreferences]' volume unit. Empty means no capacity. */
    val capacity: String = "",
    val userPreferences: UserPreferences = UserPreferences(),
    val nameError: String? = null,
    val capacityError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDiscardDialog: Boolean = false,
) {
    /** The capacity in storage units, or null when the field is empty or unusable. */
    val capacityMl: Int?
        get() = capacityToMl(capacity, userPreferences.volumeUnit)

    val hasChanges: Boolean
        get() = when {
            // Nothing to lose until the vessel we are editing has actually arrived. Without this,
            // backing out of a still-loading form prompts to discard changes that do not exist.
            isLoading -> false
            existingVessel != null -> name != existingVessel.name ||
                selectedIconName != (existingVessel.iconName ?: DEFAULT_ICON_NAME) ||
                imagePath != existingVessel.imagePath ||
                // Compared in storage units: the same physical capacity retyped in another unit is
                // not a change, and a string comparison would call it one.
                capacityMl != existingVessel.capacityMl

            else -> name.isNotBlank() ||
                selectedIconName != DEFAULT_ICON_NAME ||
                imagePath != null ||
                capacity.isNotBlank()
        }

    val isValid: Boolean
        get() = nameError == null && capacityError == null && name.isNotBlank()
}

/** The icon a vessel gets when its owner never picks one. */
internal const val DEFAULT_ICON_NAME = "generic"

// The form's field logic lives here rather than on the ViewModel because the state above needs it:
// capacityMl is derived, and a state that reached for its own ViewModel could not be built in a
// preview or a test. Pure functions so the round trip between typed text and storage is testable
// on its own, which is where the rounding bugs hide.

/** Error message for a vessel name, or null when it is acceptable. */
internal fun validateVesselName(name: String): String? = when {
    name.isBlank() -> "Name is required"
    name.length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or less"
    else -> null
}

private const val MAX_NAME_LENGTH = 50

/**
 * Error message for a capacity, or null when it is acceptable.
 *
 * Capacity is optional (`BrewingVessel.capacityMl` is nullable) so a blank field is valid, not
 * missing. The input is whatever the user typed, in whatever unit they are typing in - the bounds
 * are the same either way, so the unit does not matter here.
 */
internal fun validateCapacity(input: String): String? {
    if (input.isBlank()) return null
    val value = input.toIntOrNull() ?: return "Must be a valid number"
    return if (value <= 0) "Capacity must be greater than 0" else null
}

/**
 * The capacity as it should be stored, or null when the field is empty or unusable.
 *
 * The field holds what the user typed, in [unit]; storage is always milliliters. Converting only
 * at this boundary is what keeps the round trip stable: [VolumeUnit.fromMilliliters] rounds to
 * whole display units, so a value converted repeatedly drifts away from what the user typed.
 */
internal fun capacityToMl(input: String, unit: VolumeUnit): Int? =
    input.toIntOrNull()?.let { unit.toMilliliters(it).roundToInt() }

/** The capacity text as it should read after the display unit changes from [from] to [to]. */
internal fun convertCapacity(input: String, from: VolumeUnit, to: VolumeUnit): String {
    if (from == to) return input
    // Anything unparseable is left exactly as typed. Half-finished input is still being edited, and
    // rewriting it mid-keystroke would be worse than leaving the digits alone.
    val value = input.toIntOrNull() ?: return input
    return to.fromMilliliters(from.toMilliliters(value)).toString()
}

/** The capacity text for an already-stored vessel, shown in [unit]. */
internal fun capacityFromMl(capacityMl: Int?, unit: VolumeUnit): String =
    capacityMl?.let { unit.fromMilliliters(it.toDouble()).toString() } ?: ""
