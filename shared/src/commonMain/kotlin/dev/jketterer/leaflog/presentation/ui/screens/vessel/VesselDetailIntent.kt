package dev.jketterer.leaflog.presentation.ui.screens.vessel

sealed interface VesselDetailIntent {
    data object BackClicked : VesselDetailIntent
    data object EditVesselClicked : VesselDetailIntent
    data object BrewWithVesselClicked : VesselDetailIntent
    data class TeaClicked(val teaId: String) : VesselDetailIntent
    data object ArchiveVesselClicked : VesselDetailIntent

    /** Also the archived snackbar's Undo. */
    data object UnarchiveVesselClicked : VesselDetailIntent
    data object DeleteVesselClicked : VesselDetailIntent
    data object ConfirmDelete : VesselDetailIntent
    data object CancelDelete : VesselDetailIntent

    // In-progress session conflict dialog
    data object ResumeInProgress : VesselDetailIntent
    data object DismissInProgressDialog : VesselDetailIntent
    data object CompleteInProgressAndContinue : VesselDetailIntent
    data object DiscardInProgressAndContinue : VesselDetailIntent
}
