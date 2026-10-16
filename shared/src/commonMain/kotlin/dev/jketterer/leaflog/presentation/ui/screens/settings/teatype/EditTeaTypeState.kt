package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences
import org.jetbrains.compose.resources.StringResource

data class EditTeaTypeState(
    val isEditMode: Boolean = false,
    val existingTeaType: TeaType? = null,

    // Form fields
    val name: String = "",
    val colorHex: String = "",
    val temperatureCelsius: Double? = null,
    val userPreferences: UserPreferences = UserPreferences(),

    // Validation
    val nameError: String? = null,
    val temperatureError: StringResource? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val canDelete: Boolean = false,
    val teaCount: Int = 0,
    /** The types a type still in use can be merged into, offered instead of deleting it. */
    val mergeTargets: List<TeaType> = emptyList(),
    val mergeTargetId: String? = null,
) {
    val hasChanges: Boolean
        get() = if (isEditMode && existingTeaType != null) {
            name != existingTeaType.name ||
                    colorHex != existingTeaType.colorHex ||
                    temperatureCelsius != existingTeaType.defaultTemperatureCelsius
        } else {
            name.isNotBlank() || colorHex.isNotBlank() || temperatureCelsius != null
        }

    val isValid: Boolean
        get() = nameError == null && temperatureError == null && name.isNotBlank() && colorHex.isNotBlank()
}
