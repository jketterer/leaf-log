package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

data class EditSessionState(
    // Session metadata
    val existingSession: Session? = null,
    val existingSteep: Steep? = null, // Steep 1 when editing the session, which holds its water
    val isParentSession: Boolean = true,
    val steepNumber: Int? = null,
    val teaName: String? = null,

    // Form fields - Brewing Parameters (all sessions)
    val brewingTime: Duration? = null,
    val temperatureCelsius: String = "",
    val temperatureDisplay: String = "", // User's input in display unit (preserves exact value)

    // Form fields - Session Details (parent only)
    val selectedVessel: BrewingVessel? = null,
    val selectedWaterType: WaterType = WaterType.FILTERED,
    val waterQuantityMl: String = "",
    val waterQuantityDisplay: String = "", // User's input in display unit (preserves exact value)
    val teaQuantityGrams: String = "",
    val isTeaBag: Boolean = false,
    val location: String = "",
    val rating: Float = 0f,

    // Form fields - Notes & Photos (all sessions)
    val notes: String = "",
    val photos: List<String> = emptyList(),

    // Available options
    val availableVessels: List<BrewingVessel> = emptyList(),

    // Validation errors
    val brewingTimeError: String? = null,
    val temperatureError: String? = null,
    val vesselError: String? = null,
    val waterQuantityError: String? = null,
    val teaQuantityError: String? = null,
    val ratingError: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showSaveConfigurationDialog: Boolean = false,
    val suggestedConfigurationLabel: String = "",
    val savedSession: SessionWithSteeps? = null, // Session that was just saved
    val userPreferences: UserPreferences = UserPreferences(),
) {
    val hasChanges: Boolean
        get() {
            val session = existingSession ?: return false
            val steep = existingSteep ?: return false

            return if (isParentSession) {
                // When editing full parent session, only check parent-specific fields
                selectedVessel?.id != session.vesselId ||
                selectedWaterType != session.waterType ||
                waterQuantityMl.toDoubleOrNull() != steep.waterQuantityMl ||
                isTeaBag != (session.teaQuantityGrams == null) ||
                teaQuantityGrams.toFloatOrNull() != session.teaQuantityGrams ||
                location != (session.location ?: "")
            } else {
                // When editing steep, check steep-specific fields
                brewingTime != steep.brewingTime ||
                temperatureCelsius.toDoubleOrNull() != steep.temperatureCelsius ||
                rating != (steep.rating ?: 0f) ||
                notes != (steep.notes ?: "") ||
                photos != steep.photos
            }
        }

    val isValid: Boolean
        get() {
            return if (isParentSession) {
                // When editing full parent session, only validate parent fields
                val parentFieldsValid = selectedVessel != null &&
                    waterQuantityMl.toDoubleOrNull() != null &&
                    waterQuantityMl.toDouble() > 0

                val teaQuantityValid = isTeaBag || run {
                    val qty = teaQuantityGrams.toFloatOrNull()
                    qty != null && qty > 0
                }

                val noErrors = vesselError == null &&
                    waterQuantityError == null &&
                    teaQuantityError == null

                parentFieldsValid && teaQuantityValid && noErrors
            } else {
                // When editing steep, validate steep-specific fields
                val hasRequiredFields = brewingTime != null &&
                    brewingTime > Duration.ZERO &&
                    temperatureCelsius.toDoubleOrNull() != null

                val noErrors = brewingTimeError == null &&
                    temperatureError == null &&
                    ratingError == null

                hasRequiredFields && noErrors
            }
        }
}
