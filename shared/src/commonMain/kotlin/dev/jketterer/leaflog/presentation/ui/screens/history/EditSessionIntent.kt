package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.WaterType

sealed interface EditSessionIntent {
    // Field changes
    data class VesselSelected(val vessel: BrewingVessel) : EditSessionIntent
    data class WaterTypeSelected(val waterType: WaterType) : EditSessionIntent
    data class RinsedChanged(val rinsed: Boolean) : EditSessionIntent
    data class WaterQuantityChanged(val milliliters: Double?) : EditSessionIntent
    data class TeaQuantityChanged(val grams: Double?) : EditSessionIntent
    data class TeaBagModeChanged(val isTeaBag: Boolean) : EditSessionIntent
    data class LocationChanged(val location: String) : EditSessionIntent
    data class NotesChanged(val notes: String) : EditSessionIntent

    // Actions
    data object SaveClicked : EditSessionIntent
    data object Discard : EditSessionIntent
}
