package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

sealed interface EditTeaTypeIntent {
    data class LoadTeaType(val teaTypeId: String?) : EditTeaTypeIntent
    data class NameChanged(val name: String) : EditTeaTypeIntent
    data class ColorSelected(val colorHex: String) : EditTeaTypeIntent
    data class TemperatureChanged(val celsius: Double?) : EditTeaTypeIntent
    data object SaveClicked : EditTeaTypeIntent
    data object DeleteClicked : EditTeaTypeIntent
    data object ConfirmDelete : EditTeaTypeIntent
    data object CancelDelete : EditTeaTypeIntent
    data class MergeTargetSelected(val teaTypeId: String) : EditTeaTypeIntent
    data object ConfirmMerge : EditTeaTypeIntent
}
