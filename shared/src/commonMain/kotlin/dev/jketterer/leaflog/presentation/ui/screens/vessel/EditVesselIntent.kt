package dev.jketterer.leaflog.presentation.ui.screens.vessel

sealed interface EditVesselIntent {
    data class NameChanged(val name: String) : EditVesselIntent
    data class IconSelected(val iconName: String) : EditVesselIntent
    data class CapacityChanged(val capacityMl: Double?) : EditVesselIntent
    data class PhotoSelected(val imageBytes: ByteArray) : EditVesselIntent
    data object RemovePhoto : EditVesselIntent
    data object SaveClicked : EditVesselIntent
    data object Discard : EditVesselIntent
}
