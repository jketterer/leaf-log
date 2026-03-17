package dev.jketterer.leaflog.presentation.ui.screens.vessel

sealed interface VesselDetailIntent {
    data class LoadVessel(val vesselId: String) : VesselDetailIntent
    data object EditVesselClicked : VesselDetailIntent
    data object DeleteVesselClicked : VesselDetailIntent
    data object ConfirmDelete : VesselDetailIntent
    data object CancelDelete : VesselDetailIntent
    data object BackClicked : VesselDetailIntent
}
