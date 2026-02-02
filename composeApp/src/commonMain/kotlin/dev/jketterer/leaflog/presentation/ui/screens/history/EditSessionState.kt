package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

data class EditSessionState(
    // Session metadata
    val existingSession: TeaSession? = null,
    val isParentSession: Boolean = true,
    val steepNumber: Int? = null,
    val teaName: String? = null,

    // Form fields - Brewing Parameters (all sessions)
    val brewingTime: Duration? = null,
    val temperatureCelsius: String = "",

    // Form fields - Session Details (parent only)
    val selectedVessel: BrewingVessel? = null,
    val selectedWaterType: WaterType = WaterType.FILTERED,
    val waterQuantityMl: String = "",
    val teaQuantityGrams: String = "",
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
    val error: String? = null,
    val showDiscardDialog: Boolean = false,
    val showSaveConfigurationDialog: Boolean = false,
    val savedSession: TeaSession? = null, // Session that was just saved
    val userPreferences: UserPreferences = UserPreferences(),
) {
    val hasChanges: Boolean
        get() {
            val session = existingSession ?: return false

            return if (isParentSession) {
                // When editing full parent session, only check parent-specific fields
                selectedVessel?.id != session.vesselId ||
                selectedWaterType != session.waterType ||
                waterQuantityMl.toIntOrNull() != session.waterQuantityMl ||
                teaQuantityGrams.toFloatOrNull() != session.teaQuantityGrams ||
                location != (session.location ?: "")
            } else {
                // When editing steep, check steep-specific fields
                brewingTime != session.brewingTime ||
                temperatureCelsius.toIntOrNull() != session.temperatureCelsius ||
                rating != (session.rating ?: 0f) ||
                notes != (session.notes ?: "") ||
                photos != session.photos
            }
        }

    val isValid: Boolean
        get() {
            return if (isParentSession) {
                // When editing full parent session, only validate parent fields
                val parentFieldsValid = selectedVessel != null &&
                    waterQuantityMl.toIntOrNull() != null &&
                    waterQuantityMl.toInt() > 0

                val noErrors = vesselError == null &&
                    waterQuantityError == null &&
                    teaQuantityError == null

                parentFieldsValid && noErrors
            } else {
                // When editing steep, validate steep-specific fields
                val hasRequiredFields = brewingTime != null &&
                    brewingTime > Duration.ZERO &&
                    temperatureCelsius.toIntOrNull() != null

                val noErrors = brewingTimeError == null &&
                    temperatureError == null &&
                    ratingError == null

                hasRequiredFields && noErrors
            }
        }
}
