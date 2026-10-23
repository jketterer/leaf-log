package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.time.Duration

data class AnalyticsData(
    val totalSessions: Int,
    val totalBrewingTime: Duration,
    val totalWaterMl: Double,
    val uniqueTeasCount: Int,
    val averageRating: Float?,
    val ratedSessionsCount: Int,
    val totalTeaGrams: Double,
    val weighedSessionsCount: Int,
    val totalSteeps: Int,
    /** Teas whose first completed session falls in the window. */
    val newTeasCount: Int,
) {
    /** Mean dry leaf per brew, over the sessions that recorded a weight. */
    val averageTeaGrams: Double?
        get() = if (weighedSessionsCount > 0) totalTeaGrams / weighedSessionsCount else null

    val averageSteeps: Double?
        get() = if (totalSessions > 0) totalSteeps.toDouble() / totalSessions else null
}

data class PeriodComparison(
    val percentageChangeSessions: Float?,
    val percentageChangeBrewTime: Float?,
    val percentageChangeWater: Float?,
    val percentageChangeUniqueTeas: Float?,
    val percentageChangeAverageSteeps: Float?,
    val percentageChangeNewTeas: Float?,
    val percentageChangeTeaUsed: Float?,
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

/** A stored or backed-up name no longer here reads as [THIS_WEEK]. */
enum class AnalyticsPeriod {
    THIS_WEEK,
    THIS_MONTH,
    THIS_SEASON,
    THIS_YEAR,
    ALL_TIME,
    ;

    companion object {
        fun named(name: String?): AnalyticsPeriod = entries.find { it.name == name } ?: THIS_WEEK
    }
}

/** What one heatmap cell covers. */
enum class ActivityBucket(val unit: DateTimeUnit.DateBased) {
    DAY(DateTimeUnit.DAY),
    WEEK(DateTimeUnit.WEEK),
    MONTH(DateTimeUnit.MONTH),
    ;

    fun startOf(date: LocalDate): LocalDate = when (this) {
        DAY -> date
        WEEK -> date.startOfWeek()
        MONTH -> LocalDate(date.year, date.month, 1)
    }
}

val AnalyticsPeriod.activityBucket: ActivityBucket
    get() = when (this) {
        AnalyticsPeriod.THIS_WEEK, AnalyticsPeriod.THIS_MONTH, AnalyticsPeriod.THIS_SEASON -> ActivityBucket.DAY
        AnalyticsPeriod.THIS_YEAR -> ActivityBucket.WEEK
        AnalyticsPeriod.ALL_TIME -> ActivityBucket.MONTH
    }

/** The Monday on or before this date. Weeks start on Monday everywhere in the app. */
fun LocalDate.startOfWeek(): LocalDate = minus(dayOfWeek.ordinal, DateTimeUnit.DAY) // ordinal: MONDAY = 0
