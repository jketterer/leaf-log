package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

sealed interface TeaDetailIntent {
    data class LoadTea(val teaId: String) : TeaDetailIntent
    data object EditTeaClicked : TeaDetailIntent
    data object DeleteTeaClicked : TeaDetailIntent
    data object ConfirmDelete : TeaDetailIntent
    data object CancelDelete : TeaDetailIntent
    data object ToggleFavorite : TeaDetailIntent
    data class SessionClicked(val sessionId: String) : TeaDetailIntent
    data class EditSessionClicked(val sessionId: String) : TeaDetailIntent
    data class BrewThisTeaClicked(val vesselId: String?) : TeaDetailIntent
    data object ViewAllSessionsClicked : TeaDetailIntent
    data object BackClicked : TeaDetailIntent

    // Configuration management
    data class EditConfigurationClicked(val configId: String) : TeaDetailIntent
    data class DeleteConfigurationClicked(val configId: String) : TeaDetailIntent
    data class SaveConfigurationChanges(
        val label: String,
        val teaQuantityGrams: Float?,
        val waterQuantityMl: Double,
        val temperatureCelsius: Double,
        val brewingTime: Duration,
        val waterType: WaterType,
        val isActive: Boolean
    ) : TeaDetailIntent

    data object DismissEditConfigDialog : TeaDetailIntent
}