package dev.jketterer.leaflog.presentation.ui.screens.analytics

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import kotlinx.datetime.LocalDate

sealed interface AnalyticsIntent {
    data object LoadAnalytics : AnalyticsIntent
    data object ShowPeriodSelector : AnalyticsIntent
    data object HidePeriodSelector : AnalyticsIntent
    data class SelectPeriod(val period: AnalyticsPeriod) : AnalyticsIntent
    data class SelectCustomRange(val start: LocalDate, val end: LocalDate) : AnalyticsIntent
    data object NavigateToLogTea : AnalyticsIntent
    data class TapTrendPoint(val date: LocalDate) : AnalyticsIntent
    data class TapTeaType(val teaTypeId: String) : AnalyticsIntent
    data class TapTopTea(val teaId: String) : AnalyticsIntent
    data object ShowExportDialog : AnalyticsIntent
    data object HideExportDialog : AnalyticsIntent
    data class TapVessel(val vesselId: String) : AnalyticsIntent
    data class TapTopRatedTea(val teaId: String) : AnalyticsIntent
}
