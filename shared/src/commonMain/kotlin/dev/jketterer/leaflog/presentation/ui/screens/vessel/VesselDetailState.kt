package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.VolumeUnit

data class VesselDetailState(
    val vessel: BrewingVessel? = null,
    val volumeUnit: VolumeUnit = VolumeUnit.MILLILITERS,
    val sessionCount: Int = 0,
    val totalVesselCount: Int = 0,
    val activeVesselCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val showArchiveConfirmation: Boolean = false,
) {
    val canDelete: Boolean
        get() = totalVesselCount > 1 && sessionCount == 0

    val canArchive: Boolean
        get() = activeVesselCount > 1

    val isArchived: Boolean
        get() = vessel?.isArchived == true
}
