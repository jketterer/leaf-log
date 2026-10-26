package dev.jketterer.leaflog.presentation.ui.screens.analytics

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.RecapPeriod
import kotlinx.datetime.LocalDate

sealed interface AnalyticsIntent {
    data class SelectPeriod(val period: AnalyticsPeriod) : AnalyticsIntent
    data object NavigateToLogTea : AnalyticsIntent
    data class TapActivityCell(val date: LocalDate) : AnalyticsIntent
    data class TapTeaType(val teaTypeId: String) : AnalyticsIntent
    data class TapTea(val teaId: String) : AnalyticsIntent
    data class TapVessel(val vesselId: String) : AnalyticsIntent
    data class OpenRecap(val period: RecapPeriod) : AnalyticsIntent
}
