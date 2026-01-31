package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel

data class VesselDetailState(
    val vessel: BrewingVessel? = null,
    val sessionCount: Int = 0,
    val totalVesselCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
) {
    val canDelete: Boolean
        get() = totalVesselCount > 1 && sessionCount == 0
}
