package dev.jketterer.leaflog.presentation.ui.screens.analytics

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import kotlinx.datetime.LocalDate

sealed interface AnalyticsIntent {
    data class SelectPeriod(val period: AnalyticsPeriod) : AnalyticsIntent
    data object NavigateToLogTea : AnalyticsIntent
    data class TapTrendPoint(val date: LocalDate) : AnalyticsIntent
    data class TapTeaType(val teaTypeId: String) : AnalyticsIntent
    data class TapTopTea(val teaId: String) : AnalyticsIntent
    data class TapVessel(val vesselId: String) : AnalyticsIntent
    data class TapTopRatedTea(val teaId: String) : AnalyticsIntent
}
