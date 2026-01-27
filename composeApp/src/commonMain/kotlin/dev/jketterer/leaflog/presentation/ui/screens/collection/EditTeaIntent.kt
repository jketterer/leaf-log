package dev.jketterer.leaflog.presentation.ui.screens.collection

import kotlinx.datetime.LocalDate
import kotlin.time.Duration

sealed interface EditTeaIntent {
    data class LoadTea(val teaId: String?) : EditTeaIntent

    // Field changes
    data class NameChanged(val name: String) : EditTeaIntent
    data class TeaTypeSelected(val teaTypeId: String) : EditTeaIntent
    data class OriginChanged(val origin: String) : EditTeaIntent
    data class ProducerChanged(val producer: String) : EditTeaIntent
    data class PurchaseDateChanged(val date: LocalDate?) : EditTeaIntent
    data class PurchasePriceChanged(val price: String) : EditTeaIntent
    data class StockAmountChanged(val amount: String) : EditTeaIntent
    data class BrewingTimeChanged(val duration: Duration?) : EditTeaIntent
    data class TemperatureChanged(val temperature: String) : EditTeaIntent
    data class QuantityChanged(val quantity: String) : EditTeaIntent
    data class DescriptionChanged(val description: String) : EditTeaIntent

    // Photo management
    data object AddPhotoClicked : EditTeaIntent
    data class PhotoSelected(val photoUri: String) : EditTeaIntent
    data class PhotoRemoved(val photoUri: String) : EditTeaIntent

    // Actions
    data object SaveClicked : EditTeaIntent
    data object BackClicked : EditTeaIntent
    data object ConfirmDiscard : EditTeaIntent
    data object CancelDiscard : EditTeaIntent
}