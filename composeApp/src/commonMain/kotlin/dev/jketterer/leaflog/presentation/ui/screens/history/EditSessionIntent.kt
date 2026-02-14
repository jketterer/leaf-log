package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

sealed interface EditSessionIntent {
    data class LoadSession(
        val sessionId: String,
        val editFullSession: Boolean = false
    ) : EditSessionIntent

    // Field changes - Brewing Parameters
    data class BrewingTimeChanged(val duration: Duration) : EditSessionIntent
    data class TemperatureChanged(val temperature: String) : EditSessionIntent

    // Field changes - Session Details (parent only)
    data class VesselSelected(val vessel: BrewingVessel) : EditSessionIntent
    data class WaterTypeSelected(val waterType: WaterType) : EditSessionIntent
    data class WaterQuantityChanged(val quantity: String) : EditSessionIntent
    data class TeaQuantityChanged(val quantity: String) : EditSessionIntent
    data class LocationChanged(val location: String) : EditSessionIntent
    data class RatingChanged(val rating: Float) : EditSessionIntent

    // Field changes - Notes & Photos
    data class NotesChanged(val notes: String) : EditSessionIntent
    data class PhotoSelected(val imageBytes: ByteArray) : EditSessionIntent
    data class PhotoRemoved(val path: String) : EditSessionIntent

    // Actions
    data object SaveClicked : EditSessionIntent
    data object BackClicked : EditSessionIntent
    data object ConfirmDiscard : EditSessionIntent
    data object CancelDiscard : EditSessionIntent

    // Configuration save dialog
    data class SaveConfigurationClicked(val customLabel: String?) : EditSessionIntent
    data object SkipSaveConfiguration : EditSessionIntent
}
