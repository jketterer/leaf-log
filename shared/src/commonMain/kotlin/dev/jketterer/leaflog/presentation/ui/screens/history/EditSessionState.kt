package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import org.jetbrains.compose.resources.StringResource

data class EditSessionState(
    // Session metadata
    val existingSession: Session? = null,
    val existingSteep: Steep? = null, // Steep 1, which holds the session's water

    // Form fields
    val selectedVessel: BrewingVessel? = null,
    val selectedWaterType: WaterType = WaterType.FILTERED,
    val rinsed: Boolean = false,
    val waterQuantityMl: Double? = null,
    val teaQuantityGrams: Double? = null,
    val isTeaBag: Boolean = false,
    val location: String = "",
    val notes: String = "",

    // Available options
    val availableVessels: List<BrewingVessel> = emptyList(),

    // Validation errors
    val vesselError: String? = null,
    val waterQuantityError: StringResource? = null,
    val teaQuantityError: StringResource? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val userPreferences: UserPreferences = UserPreferences(),
) {
    val hasChanges: Boolean
        get() {
            val session = existingSession ?: return false
            val steep = existingSteep ?: return false

            return selectedVessel?.id != session.vesselId ||
                selectedWaterType != session.waterType ||
                rinsed != session.rinsed ||
                waterQuantityMl != steep.waterQuantityMl ||
                isTeaBag != (session.teaQuantityGrams == null) ||
                teaQuantityGrams?.toFloat() != session.teaQuantityGrams ||
                location != (session.location ?: "") ||
                notes != (session.notes ?: "")
        }

    val isValid: Boolean
        get() {
            val fieldsValid = selectedVessel != null &&
                waterQuantityMl != null &&
                waterQuantityMl > 0

            val teaQuantityValid = isTeaBag || (teaQuantityGrams != null && teaQuantityGrams > 0)

            val noErrors = vesselError == null &&
                waterQuantityError == null &&
                teaQuantityError == null

            return fieldsValid && teaQuantityValid && noErrors
        }
}
