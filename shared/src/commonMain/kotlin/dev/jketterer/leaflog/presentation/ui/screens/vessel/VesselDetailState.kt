package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.VesselProfile
import dev.jketterer.leaflog.presentation.ui.viewmodel.InProgressDialogState

data class VesselDetailState(
    val vessel: BrewingVessel? = null,
    /** Null until a session brewed in it is completed. */
    val profile: VesselProfile? = null,
    /** Any session, completed or not, keeps it from being deleted. */
    val hasSessions: Boolean = false,
    val isLoading: Boolean = true,
    val showDeleteConfirmation: Boolean = false,
    val inProgressDialogState: InProgressDialogState? = null,
)
