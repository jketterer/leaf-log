package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

data class EditTeaState(
    val isEditMode: Boolean = false,
    val existingTea: Tea? = null,

    // form fields
    val name: String = "",
    val selectedTeaTypeId: String? = null,
    val origin: String = "",
    val producer: String = "",
    val purchaseDate: LocalDate? = null,
    val defaultTemperatureCelsius: Double? = null,
    // Which tea type seeded the temperature, or null if the user typed it. Lets a mis-tapped type
    // be corrected without stranding its default, while never overwriting a chosen value.
    val temperatureFromTeaTypeId: String? = null,
    val description: String = "",
    val photos: List<String> = emptyList(),

    // Available options
    val availableTeaTypes: List<TeaType> = emptyList(),
    val availableProducers: List<String> = emptyList(),

    // Validation errors
    val nameError: String? = null,
    val teaTypeError: String? = null,
    val temperatureError: StringResource? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,

    // User preferences
    val userPreferences: UserPreferences = UserPreferences(),
) {
    val hasChanges: Boolean
        get() = if (isEditMode && existingTea != null) {
            name != existingTea.name ||
                    selectedTeaTypeId != existingTea.teaTypeId ||
                    origin != (existingTea.origin ?: "") ||
                    producer != (existingTea.producer ?: "") ||
                    purchaseDate != existingTea.purchaseDate ||
                    defaultTemperatureCelsius != existingTea.defaultTemperatureCelsius ||
                    description != (existingTea.description ?: "") ||
                    photos != existingTea.photos
        } else {
            name.isNotBlank() ||
                    selectedTeaTypeId != null ||
                    origin.isNotBlank() ||
                    producer.isNotBlank() ||
                    purchaseDate != null ||
                    defaultTemperatureCelsius != null ||
                    description.isNotBlank() ||
                    photos.isNotEmpty()
        }

    val isValid: Boolean
        get() = nameError == null &&
                teaTypeError == null &&
                temperatureError == null &&
                name.isNotBlank() &&
                selectedTeaTypeId != null
}