package dev.jketterer.leaflog.presentation.ui.screens.log

import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerFilter
import kotlin.time.Duration

sealed interface LogTeaIntent {
    data object LoadData : LogTeaIntent

    // Tea selection
    data object ShowTeaPicker : LogTeaIntent
    data object HideTeaPicker : LogTeaIntent
    data class TeaPickerQueryChanged(val query: String) : LogTeaIntent
    data class TeaPickerFilterChanged(val filter: TeaPickerFilter) : LogTeaIntent
    data class TeaSelected(val teaId: String?) : LogTeaIntent
    data class QuickAddTeaSaved(val name: String, val teaTypeId: String) : LogTeaIntent

    // Parameter changes
    data class TeaBagModeChanged(val isTeaBag: Boolean) : LogTeaIntent
    data class TeaQuantityChanged(val grams: Double?) : LogTeaIntent
    data class WaterQuantityChanged(val milliliters: Double?) : LogTeaIntent
    data class TemperatureChanged(val celsius: Double?) : LogTeaIntent
    data class BrewingTimeChanged(val duration: Duration) : LogTeaIntent
    data class VesselSelected(val vesselId: String?) : LogTeaIntent
    data class WaterTypeSelected(val waterType: WaterType) : LogTeaIntent
    data class RinsedChanged(val rinsed: Boolean) : LogTeaIntent
    data class LocationChanged(val location: String) : LogTeaIntent

    // Photo management
    data object AddPhotoClicked : LogTeaIntent
    data class PhotoSelected(val photoUri: String) : LogTeaIntent
    data class PhotoRemoved(val photoUri: String) : LogTeaIntent

    // Actions
    data object SaveAsCompleted : LogTeaIntent
    data object StartTimerClicked : LogTeaIntent
    data object BackClicked : LogTeaIntent
    data class SourceBrewClicked(val sessionId: String) : LogTeaIntent

    /** The brew a session started here replays: the pair's last or best, or null for scratch. */
    data class SourceBrewSelected(val sessionId: String?) : LogTeaIntent

    // In-progress session conflict dialog
    data object ResumeInProgress : LogTeaIntent
    data object DismissInProgressDialog : LogTeaIntent
    data object CompleteInProgressAndContinue : LogTeaIntent
    data object DiscardInProgressAndContinue : LogTeaIntent
}