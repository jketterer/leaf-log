package dev.jketterer.leaflog.presentation.ui.screens.analytics

import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.AnalyticsData
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.PeriodComparison
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TopRatedTea
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.domain.models.TrendGranularity
import dev.jketterer.leaflog.domain.models.TrendPoint
import dev.jketterer.leaflog.domain.models.VesselDistribution

/** Completed sessions needed before Analytics has enough to show. */
internal const val MINIMUM_SESSIONS = 10

data class AnalyticsState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_WEEK,
    val analytics: AnalyticsData? = null,
    val comparison: PeriodComparison? = null,
    val periodLabel: String = "This Week",
    val isLoading: Boolean = true,
    val hasMinimumData: Boolean = false,
    val totalCompletedSessions: Int = 0,
    val showPeriodSelector: Boolean = false,
    val trendPoints: List<TrendPoint> = emptyList(),
    val trendGranularity: TrendGranularity = TrendGranularity.DAILY,
    val teaTypeDistribution: List<TeaTypeDistribution> = emptyList(),
    val topTeas: List<TopTea> = emptyList(),
    val activityCells: List<ActivityCell> = emptyList(),
    val topRatedTeas: List<TopRatedTea> = emptyList(),
    val vesselDistribution: List<VesselDistribution> = emptyList(),
)
