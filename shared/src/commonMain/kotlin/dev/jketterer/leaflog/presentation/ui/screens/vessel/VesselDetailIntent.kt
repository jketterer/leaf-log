package dev.jketterer.leaflog.presentation.ui.screens.vessel

sealed interface VesselDetailIntent {
    data class LoadVessel(val vesselId: String) : VesselDetailIntent
    data object EditVesselClicked : VesselDetailIntent
    data object DeleteVesselClicked : VesselDetailIntent
    data object ConfirmDelete : VesselDetailIntent
    data object CancelDelete : VesselDetailIntent
    data object ArchiveVesselClicked : VesselDetailIntent
    data object ConfirmArchive : VesselDetailIntent
    data object CancelArchive : VesselDetailIntent
    data object UnarchiveVesselClicked : VesselDetailIntent
    data object BackClicked : VesselDetailIntent
}
