package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import kotlin.time.Duration

sealed interface EditTeaTypeIntent {
    data class LoadTeaType(val teaTypeId: String?) : EditTeaTypeIntent
    data class NameChanged(val name: String) : EditTeaTypeIntent
    data class ColorSelected(val colorHex: String) : EditTeaTypeIntent
    data class TemperatureChanged(val temperature: String) : EditTeaTypeIntent
    data class BrewTimeChanged(val duration: Duration?) : EditTeaTypeIntent
    data object SaveClicked : EditTeaTypeIntent
    data object BackClicked : EditTeaTypeIntent
    data object ConfirmDiscard : EditTeaTypeIntent
    data object CancelDiscard : EditTeaTypeIntent
    data object DeleteClicked : EditTeaTypeIntent
    data object ConfirmDelete : EditTeaTypeIntent
    data object CancelDelete : EditTeaTypeIntent
}
