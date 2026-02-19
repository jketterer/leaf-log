package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.Duration

data class AnalyticsData(
    val totalSessions: Int,
    val totalBrewingTime: Duration,
    val totalWaterMl: Double,
    val uniqueTeasCount: Int,
    val averageRating: Float?,
    val ratedSessionsCount: Int,
)

data class PeriodComparison(
    val previousSessions: Int,
    val percentageChange: Float,
)

data class Insight(
    val type: InsightType,
    val text: String,
)

enum class InsightType {
    PERIOD_COMPARISON,
    MOST_BREWED,
    AVERAGE_RATING,
    CONSISTENCY,
    VARIETY,
}

data class TrendPoint(
    val date: LocalDate,
    val sessionCount: Int,
)

data class TeaTypeDistribution(
    val teaType: TeaType,
    val sessionCount: Int,
    val percentage: Float,
)

data class TopTea(
    val tea: Tea,
    val sessionCount: Int,
)

enum class AnalyticsPeriod {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    ALL_TIME,
    CUSTOM,
    ;

    val label: String
        get() = when (this) {
            LAST_7_DAYS -> "Last 7 Days"
            LAST_30_DAYS -> "Last 30 Days"
            LAST_90_DAYS -> "Last 90 Days"
            THIS_WEEK -> "This Week"
            THIS_MONTH -> "This Month"
            LAST_MONTH -> "Last Month"
            THIS_YEAR -> "This Year"
            ALL_TIME -> "All Time"
            CUSTOM -> "Custom Range"
        }
}
