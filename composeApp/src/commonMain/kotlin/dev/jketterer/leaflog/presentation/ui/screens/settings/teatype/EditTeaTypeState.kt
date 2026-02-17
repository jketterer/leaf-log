package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UnitConverter
import dev.jketterer.leaflog.domain.models.UserPreferences
import kotlin.time.Duration

data class EditTeaTypeState(
    val isEditMode: Boolean = false,
    val existingTeaType: TeaType? = null,

    // Form fields
    val name: String = "",
    val colorHex: String = "",
    val temperature: String = "",
    val brewTime: Duration? = null,
    val userPreferences: UserPreferences = UserPreferences(),

    // Validation
    val nameError: String? = null,
    val temperatureError: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDiscardDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val canDelete: Boolean = false,
    val teaCount: Int = 0,
) {
    val hasChanges: Boolean
        get() = if (isEditMode && existingTeaType != null) {
            val existingTemp = existingTeaType.defaultTemperatureCelsius?.let {
                UnitConverter.celsiusToDisplayTemperature(it, userPreferences.temperatureUnit)
            }?.toString() ?: ""
            name != existingTeaType.name ||
                    colorHex != existingTeaType.colorHex ||
                    temperature != existingTemp ||
                    brewTime != existingTeaType.defaultBrewingTime
        } else {
            name.isNotBlank() || colorHex.isNotBlank() || temperature.isNotBlank() || brewTime != null
        }

    val isValid: Boolean
        get() = nameError == null && temperatureError == null && name.isNotBlank() && colorHex.isNotBlank()
}
