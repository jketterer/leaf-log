package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences
import kotlinx.datetime.LocalDate

data class EditTeaState(
    val isEditMode: Boolean = false,
    val existingTea: Tea? = null,

    // form fields
    val name: String = "",
    val selectedTeaTypeId: String? = null,
    val origin: String = "",
    val producer: String = "",
    val purchaseDate: LocalDate? = null,
    val defaultTemperatureCelsius: String = "",
    val defaultTemperatureDisplay: String = "", // User's input in display unit (preserves exact value)
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
    val temperatureError: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDiscardDialog: Boolean = false,

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
                    defaultTemperatureCelsius != (existingTea.defaultTemperatureCelsius?.toString()
                ?: "") ||
                    description != (existingTea.description ?: "") ||
                    photos != existingTea.photos
        } else {
            name.isNotBlank() ||
                    selectedTeaTypeId != null ||
                    origin.isNotBlank() ||
                    producer.isNotBlank() ||
                    purchaseDate != null ||
                    defaultTemperatureCelsius.isNotBlank() ||
                    description.isNotBlank() ||
                    photos.isNotEmpty()
        }

    /**
     * The temperature as the field should show it.
     *
     * Prefers exactly what the user typed; falls back to converting the stored Celsius into their
     * preferred unit, which is how a loaded tea - or a value seeded from the tea type - gets shown.
     */
    val temperatureDisplayValue: String
        get() = defaultTemperatureDisplay.ifEmpty {
            defaultTemperatureCelsius.toDoubleOrNull()
                ?.let { userPreferences.temperatureUnit.fromCelsius(it).toString() }
                ?: defaultTemperatureCelsius
        }

    val isValid: Boolean
        get() = nameError == null &&
                teaTypeError == null &&
                temperatureError == null &&
                name.isNotBlank() &&
                selectedTeaTypeId != null
}