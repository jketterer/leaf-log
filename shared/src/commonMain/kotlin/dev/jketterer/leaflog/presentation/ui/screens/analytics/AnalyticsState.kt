package dev.jketterer.leaflog.presentation.ui.screens.analytics

import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.AnalyticsData
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.PeriodComparison
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TopRatedTea
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.domain.models.VesselDistribution

data class AnalyticsState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_WEEK,
    val analytics: AnalyticsData? = null,
    val comparison: PeriodComparison? = null,
    val isLoading: Boolean = true,
    /** Whether any session was ever completed, whatever the period. */
    val hasCompletedSession: Boolean = false,
    val teaTypeDistribution: List<TeaTypeDistribution> = emptyList(),
    val topTeas: List<TopTea> = emptyList(),
    val activityCells: List<ActivityCell> = emptyList(),
    val topRatedTeas: List<TopRatedTea> = emptyList(),
    val vesselDistribution: List<VesselDistribution> = emptyList(),
)
