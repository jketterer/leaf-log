package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel

data class EditVesselState(
    val isEditMode: Boolean = false,
    val existingVessel: BrewingVessel? = null,
    val name: String = "",
    val selectedIconName: String? = null,
    val capacityMl: String = "",
    val nameError: String? = null,
    val capacityError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDiscardDialog: Boolean = false,
) {
    val hasChanges: Boolean
        get() = if (isEditMode && existingVessel != null) {
            name != existingVessel.name ||
            selectedIconName != existingVessel.iconName ||
            capacityMl != (existingVessel.capacityMl?.toString() ?: "")
        } else {
            name.isNotBlank() || selectedIconName != null || capacityMl.isNotBlank()
        }

    val isValid: Boolean
        get() = nameError == null && capacityError == null && name.isNotBlank() && selectedIconName != null
}
