package dev.jketterer.leaflog.presentation.ui.screens.vessel

sealed interface EditVesselIntent {
    data class LoadVessel(val vesselId: String?) : EditVesselIntent
    data class NameChanged(val name: String) : EditVesselIntent
    data class IconSelected(val iconName: String) : EditVesselIntent
    data class CapacityChanged(val capacity: String) : EditVesselIntent
    data object SaveClicked : EditVesselIntent
    data object BackClicked : EditVesselIntent
    data object ConfirmDiscard : EditVesselIntent
    data object CancelDiscard : EditVesselIntent
}
