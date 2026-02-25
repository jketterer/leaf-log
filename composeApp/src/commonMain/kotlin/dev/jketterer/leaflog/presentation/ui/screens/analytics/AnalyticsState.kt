package dev.jketterer.leaflog.presentation.ui.screens.analytics

import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.AnalyticsData
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.Insight
import dev.jketterer.leaflog.domain.models.PeriodComparison
import dev.jketterer.leaflog.domain.models.SteepInsights
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TopRatedTea
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.domain.models.TrendPoint
import dev.jketterer.leaflog.domain.models.VesselDistribution
import kotlinx.datetime.LocalDate

data class AnalyticsState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_MONTH,
    val customDateRange: Pair<LocalDate, LocalDate>? = null,
    val analytics: AnalyticsData? = null,
    val comparison: PeriodComparison? = null,
    val insights: List<Insight> = emptyList(),
    val formattedWaterQuantity: String = "",
    val formattedBrewingTime: String = "",
    val periodLabel: String = "This Month",
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasMinimumData: Boolean = false,
    val totalCompletedSessions: Int = 0,
    val showPeriodSelector: Boolean = false,
    val trendPoints: List<TrendPoint> = emptyList(),
    val teaTypeDistribution: List<TeaTypeDistribution> = emptyList(),
    val topTeas: List<TopTea> = emptyList(),
    val showExportDialog: Boolean = false,
    val exportCsvContent: String? = null,
    val activityCells: List<ActivityCell> = emptyList(),
    val steepInsights: SteepInsights? = null,
    val topRatedTeas: List<TopRatedTea> = emptyList(),
    val vesselDistribution: List<VesselDistribution> = emptyList(),
)
