package dev.jketterer.leaflog.presentation.ui.screens.vessel

sealed interface VesselListIntent {
    data object LoadVessels : VesselListIntent
    data class VesselClicked(val vesselId: String) : VesselListIntent
    data object AddVesselClicked : VesselListIntent
    data object ClearError : VesselListIntent
    data object BackClicked : VesselListIntent
}
