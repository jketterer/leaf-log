package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.UnitConverter
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit

data class EditVesselState(
    val isEditMode: Boolean = false,
    val existingVessel: BrewingVessel? = null,
    val name: String = "",
    val selectedIconName: String? = null,
    val imagePath: String? = null,
    val capacity: String = "",
    val userPreferences: UserPreferences = UserPreferences(),
    val nameError: String? = null,
    val capacityError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDiscardDialog: Boolean = false,
) {
    val hasChanges: Boolean
        get() = if (isEditMode && existingVessel != null) {
            val vesselCapacity = existingVessel.capacityMl?.let {
                when (userPreferences.volumeUnit) {
                    VolumeUnit.MILLILITERS -> existingVessel.capacityMl
                    else -> UnitConverter.millilitersToDisplayVolume(
                        existingVessel.capacityMl.toDouble(),
                        userPreferences.volumeUnit,
                    )
                }.toString()
            } ?: ""
            name != existingVessel.name ||
                    selectedIconName != existingVessel.iconName ||
                    imagePath != existingVessel.imagePath ||
                    capacity != (vesselCapacity)
        } else {
            name.isNotBlank() || selectedIconName != null || imagePath != null || capacity.isNotBlank()
        }

    val isValid: Boolean
        get() = nameError == null && capacityError == null && name.isNotBlank() && selectedIconName != null
}
