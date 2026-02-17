package dev.jketterer.leaflog.presentation.ui.screens.log

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

sealed interface LogTeaIntent {
    data object LoadData : LogTeaIntent

    // Tea selection
    data object ShowTeaSearchDialog : LogTeaIntent
    data object HideTeaSearchDialog : LogTeaIntent
    data class TeaSearchQueryChanged(val query: String) : LogTeaIntent
    data class TeaSelected(val teaId: String?) : LogTeaIntent
    data object QuickAddTeaClicked : LogTeaIntent
    data class QuickAddTeaSaved(val tea: Tea) : LogTeaIntent

    // Parameter changes
    data class TeaQuantityChanged(val quantity: String) : LogTeaIntent
    data class WaterQuantityChanged(val quantity: String) : LogTeaIntent
    data class TemperatureChanged(val temperature: String) : LogTeaIntent
    data class BrewingTimeChanged(val duration: Duration) : LogTeaIntent
    data class VesselSelected(val vesselId: String?) : LogTeaIntent
    data class WaterTypeSelected(val waterType: WaterType) : LogTeaIntent
    data class LocationChanged(val location: String) : LogTeaIntent
    data class NotesChanged(val notes: String) : LogTeaIntent
    data object ToggleTemperatureUnit : LogTeaIntent
    data object ToggleVolumeUnit : LogTeaIntent

    // Photo management
    data object AddPhotoClicked : LogTeaIntent
    data class PhotoSelected(val photoUri: String) : LogTeaIntent
    data class PhotoRemoved(val photoUri: String) : LogTeaIntent

    // Actions
    data object SaveAsDraft : LogTeaIntent
    data object SaveAsCompleted : LogTeaIntent
    data object StartTimerClicked : LogTeaIntent
    data object BackClicked : LogTeaIntent

    // Configuration selection
    data object ChooseDifferentMethodClicked : LogTeaIntent
    data class MethodSelected(val configurationId: String?) : LogTeaIntent
    data object DismissChooseMethodDialog : LogTeaIntent
}