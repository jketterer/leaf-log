package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

data class EditTeaState(
    val isEditMode: Boolean = false,
    val existingTea: Tea? = null,

    // form fields
    val name: String = "",
    val selectedTeaTypeId: String? = null,
    val origin: String = "",
    val producer: String = "",
    val purchaseDate: LocalDate? = null,
    val purchasePrice: String = "",
    val stockAmount: String = "",
    val defaultBrewingTime: Duration? = null,
    val defaultTemperatureCelsius: String = "",
    val defaultQuantity: String = "",
    val description: String = "",
    val photos: List<String> = emptyList(),

    // Available options
    val availableTeaTypes: List<TeaType> = emptyList(),

    // Validation errors
    val nameError: String? = null,
    val teaTypeError: String? = null,
    val purchasePriceError: String? = null,
    val stockAmountError: String? = null,
    val temperatureError: String? = null,
    val quantityError: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDiscardDialog: Boolean = false
) {
    val hasChanges: Boolean
        get() = if (isEditMode && existingTea != null) {
            name != existingTea.name ||
                    selectedTeaTypeId != existingTea.teaTypeId ||
                    origin != (existingTea.origin ?: "") ||
                    producer != (existingTea.producer ?: "") ||
                    purchaseDate != existingTea.purchaseDate ||
                    purchasePrice != (existingTea.purchasePrice?.toString() ?: "") ||
                    stockAmount != (existingTea.stockAmount?.toString() ?: "") ||
                    defaultTemperatureCelsius != (existingTea.defaultTemperatureCelsius?.toString()
                ?: "") ||
                    defaultQuantity != (existingTea.defaultQuantity?.toString() ?: "") ||
                    description != (existingTea.description ?: "") ||
                    photos != existingTea.photos
        } else {
            name.isNotBlank() ||
                    selectedTeaTypeId != null ||
                    origin.isNotBlank() ||
                    producer.isNotBlank() ||
                    purchaseDate != null ||
                    purchasePrice.isNotBlank() ||
                    stockAmount.isNotBlank() ||
                    defaultTemperatureCelsius.isNotBlank() ||
                    defaultQuantity.isNotBlank() ||
                    description.isNotBlank() ||
                    photos.isNotEmpty()
        }

    val isValid: Boolean
        get() = nameError == null &&
                teaTypeError == null &&
                purchasePriceError == null &&
                stockAmountError == null &&
                temperatureError == null &&
                quantityError == null &&
                name.isNotBlank() &&
                selectedTeaTypeId != null
}