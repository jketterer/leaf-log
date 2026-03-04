package dev.jketterer.leaflog.presentation.ui.screens.collection

import kotlinx.datetime.LocalDate

sealed interface EditTeaIntent {
    data class LoadTea(val teaId: String?) : EditTeaIntent

    // Field changes
    data class NameChanged(val name: String) : EditTeaIntent
    data class TeaTypeSelected(val teaTypeId: String) : EditTeaIntent
    data class OriginChanged(val origin: String) : EditTeaIntent
    data class ProducerChanged(val producer: String) : EditTeaIntent
    data class PurchaseDateChanged(val date: LocalDate?) : EditTeaIntent
    data class TemperatureChanged(val temperature: String) : EditTeaIntent
    data class DescriptionChanged(val description: String) : EditTeaIntent

    // Photo management
    data class PhotoSelected(val imageBytes: ByteArray) : EditTeaIntent
    data class PhotoRemoved(val photoPath: String) : EditTeaIntent

    // UI toggles
    data object ToggleTemperatureUnit : EditTeaIntent

    // Actions
    data object SaveClicked : EditTeaIntent
    data object BackClicked : EditTeaIntent
    data object ConfirmDiscard : EditTeaIntent
    data object CancelDiscard : EditTeaIntent
}