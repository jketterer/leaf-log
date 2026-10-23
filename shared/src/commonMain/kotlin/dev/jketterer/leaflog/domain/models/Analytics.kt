package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Instant

/** The instants `[start, end)`, whose days are read in [tz]. */
data class DateWindow(
    val start: Instant,
    val end: Instant,
    val tz: TimeZone,
) {
    operator fun contains(instant: Instant): Boolean = instant in start..<end

    val firstDate: LocalDate get() = start.toLocalDateTime(tz).date

    /** The last day the window reaches into: an end at midnight leaves that day out. */
    val lastDate: LocalDate
        get() {
            val end = end.toLocalDateTime(tz)
            return if (end.time.toNanosecondOfDay() == 0L) end.date.minus(
                1,
                DateTimeUnit.DAY
            ) else end.date
        }
}

/** The window a period covers, and the one it is compared against; All time has none. */
data class PeriodWindows(
    val current: DateWindow,
    val previous: DateWindow?,
)

/** The windows this period covers at [now] in [tz]. */
fun AnalyticsPeriod.window(now: Instant, tz: TimeZone): PeriodWindows {
    val today = now.toLocalDateTime(tz).date
    fun midnight(date: LocalDate) = date.atStartOfDayIn(tz)

    // A calendar period is compared with the same elapsed part of the one before: on a Monday,
    // this week is compared with last Monday only
    fun calendar(first: LocalDate, unit: DateTimeUnit.DateBased, end: Instant) = PeriodWindows(
        current = DateWindow(midnight(first), end, tz),
        previous = DateWindow(midnight(first.minus(1, unit)), now.minus(1, unit, tz), tz),
    )

    fun spanning(first: LocalDate, unit: DateTimeUnit.DateBased) =
        calendar(first, unit, midnight(first.plus(1, unit)))

    val firstOfMonth = LocalDate(today.year, today.month, 1)
    return when (this) {
        AnalyticsPeriod.THIS_WEEK -> spanning(today.startOfWeek(), DateTimeUnit.WEEK)
        AnalyticsPeriod.THIS_MONTH -> spanning(firstOfMonth, DateTimeUnit.MONTH)
        // Meteorological seasons begin in December, March, June and September
        AnalyticsPeriod.THIS_SEASON ->
            spanning(firstOfMonth.minus((today.month.ordinal + 1) % 3, DateTimeUnit.MONTH), SEASON)

        AnalyticsPeriod.THIS_YEAR -> calendar(LocalDate(today.year, 1, 1), DateTimeUnit.YEAR, now)
        AnalyticsPeriod.ALL_TIME -> PeriodWindows(DateWindow(Instant.DISTANT_PAST, now, tz), null)
    }
}

private val SEASON = DateTimeUnit.MONTH * 3

// Each aggregator reads every session, counting the completed ones that started in the window

private fun List<SessionWithSteeps>.completedIn(window: DateWindow) =
    filter { it.session.status == SessionStatus.COMPLETED && it.startedAt in window }

fun List<SessionWithSteeps>.summary(window: DateWindow): AnalyticsData {
    val sessions = completedIn(window)
    val steeps = sessions.flatMap { it.steeps }
    // Water is per steep, each one fresh. Leaf is per session: every steep re-uses it.
    val weighed = sessions.mapNotNull { it.session.teaQuantityGrams }
    val ratings = sessions.mapNotNull { it.effectiveRating }
    val firstBrews = filter { it.session.status == SessionStatus.COMPLETED }
        .groupBy { it.session.teaId }
        .values.map { teaSessions -> teaSessions.minOf { it.startedAt } }
    return AnalyticsData(
        totalSessions = sessions.size,
        totalBrewingTime = steeps.fold(Duration.ZERO) { total, steep -> total + steep.brewingTime },
        totalWaterMl = steeps.sumOf { it.waterQuantityMl },
        uniqueTeasCount = sessions.distinctBy { it.session.teaId }.size,
        averageRating = ratings.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        ratedSessionsCount = ratings.size,
        totalTeaGrams = weighed.sumOf { it.toDouble() },
        weighedSessionsCount = weighed.size,
        totalSteeps = steeps.size,
        newTeasCount = firstBrews.count { it in window },
    )
}

/** Each figure's percent change from [previous], or null when [previous] had no sessions. */
fun AnalyticsData.comparedTo(previous: AnalyticsData): PeriodComparison? {
    if (previous.totalSessions == 0) return null
    fun change(current: Double, before: Double): Float? =
        if (before == 0.0) null else ((current - before) / before * 100).toFloat()
    return PeriodComparison(
        percentageChangeSessions = change(
            totalSessions.toDouble(),
            previous.totalSessions.toDouble()
        ),
        percentageChangeBrewTime = change(
            totalBrewingTime.inWholeSeconds.toDouble(),
            previous.totalBrewingTime.inWholeSeconds.toDouble(),
        ),
        percentageChangeWater = change(totalWaterMl, previous.totalWaterMl),
        percentageChangeUniqueTeas = change(
            uniqueTeasCount.toDouble(),
            previous.uniqueTeasCount.toDouble()
        ),
        percentageChangeAverageSteeps = averageSteeps?.let {
            change(
                it,
                previous.averageSteeps ?: 0.0
            )
        },
        percentageChangeNewTeas = change(newTeasCount.toDouble(), previous.newTeasCount.toDouble()),
        percentageChangeTeaUsed = change(totalTeaGrams, previous.totalTeaGrams),
    )
}

/**
 * A cell per day, or per Monday-led week with the first clamped to the window's first day, each
 * colored by its most brewed tea type.
 */
fun List<SessionWithSteeps>.activity(
    window: DateWindow,
    byWeek: Boolean,
    teas: Map<String, Tea>,
    teaTypes: Map<String, TeaType>,
): List<ActivityCell> {
    val bucket: (LocalDate) -> LocalDate = if (byWeek) LocalDate::startOfWeek else { date -> date }
    val unit = if (byWeek) DateTimeUnit.WEEK else DateTimeUnit.DAY
    val sessionsByBucket =
        completedIn(window).groupBy { bucket(it.startedAt.toLocalDateTime(window.tz).date) }
    val lastDate = window.lastDate
    return generateSequence(bucket(window.firstDate)) { it.plus(1, unit) }
        .takeWhile { it <= lastDate }
        .map { start ->
            val sessions = sessionsByBucket[start].orEmpty()
            ActivityCell(
                date = maxOf(start, window.firstDate),
                dominantTeaTypeColorHex = sessions
                    .mapNotNull { teas[it.session.teaId]?.let { tea -> teaTypes[tea.teaTypeId]?.colorHex } }
                    .filter { it.isNotEmpty() }
                    .groupingBy { it }.eachCount()
                    .maxByOrNull { it.value }?.key,
                sessionCount = sessions.size,
            )
        }
        .toList()
}

/** Sessions per day, week or month, from the window's first day or first session if later. */
fun List<SessionWithSteeps>.trends(window: DateWindow): BrewingTrends {
    val dates = completedIn(window).map { it.startedAt.toLocalDateTime(window.tz).date }
    if (dates.isEmpty()) return BrewingTrends(emptyList(), TrendGranularity.DAILY)

    val startDate = maxOf(window.firstDate, dates.min())
    val endDate = window.end.toLocalDateTime(window.tz).date
    val days = startDate.daysUntil(endDate)
    val granularity = when {
        days <= 45 -> TrendGranularity.DAILY
        days <= 180 -> TrendGranularity.WEEKLY
        else -> TrendGranularity.MONTHLY
    }
    val bucket: (LocalDate) -> LocalDate = when (granularity) {
        TrendGranularity.DAILY -> { date -> date }
        TrendGranularity.WEEKLY -> LocalDate::startOfWeek
        TrendGranularity.MONTHLY -> { date -> LocalDate(date.year, date.month, 1) }
    }
    val unit = when (granularity) {
        TrendGranularity.DAILY -> DateTimeUnit.DAY
        TrendGranularity.WEEKLY -> DateTimeUnit.WEEK
        TrendGranularity.MONTHLY -> DateTimeUnit.MONTH
    }
    val counts = dates.groupingBy(bucket).eachCount()
    val points = generateSequence(bucket(startDate)) { it.plus(1, unit) }
        .takeWhile { it <= endDate }
        .map { TrendPoint(it, counts[it] ?: 0) }
        .toList()
    return BrewingTrends(points, granularity)
}

/** Tea types by session count, most first. */
fun List<SessionWithSteeps>.teaTypeDistribution(
    window: DateWindow,
    teas: Map<String, Tea>,
    teaTypes: Map<String, TeaType>,
): List<TeaTypeDistribution> {
    val sessions = completedIn(window)
    return sessions.mapNotNull { teas[it.session.teaId]?.teaTypeId }
        .groupingBy { it }.eachCount()
        .mapNotNull { (teaTypeId, count) ->
            teaTypes[teaTypeId]?.let {
                TeaTypeDistribution(
                    it,
                    count,
                    count * 100f / sessions.size
                )
            }
        }
        .sortedByDescending { it.sessionCount }
}

/** Vessels by session count, most first, out of the sessions that recorded one. */
fun List<SessionWithSteeps>.vesselDistribution(
    window: DateWindow,
    vessels: Map<String, BrewingVessel>,
): List<VesselDistribution> {
    val sessions = completedIn(window).filter { it.session.vesselId.isNotBlank() }
    return sessions.groupingBy { it.session.vesselId }.eachCount()
        .mapNotNull { (vesselId, count) ->
            vessels[vesselId]?.let { VesselDistribution(it, count, count * 100f / sessions.size) }
        }
        .sortedByDescending { it.sessionCount }
}

/** The 5 most brewed teas. */
fun List<SessionWithSteeps>.topTeas(window: DateWindow, teas: Map<String, Tea>): List<TopTea> =
    completedIn(window).groupingBy { it.session.teaId }.eachCount()
        .mapNotNull { (teaId, count) -> teas[teaId]?.let { TopTea(it, count) } }
        .sortedByDescending { it.sessionCount }
        .take(5)

/** The 5 teas with the highest mean session rating. */
fun List<SessionWithSteeps>.topRatedTeas(
    window: DateWindow,
    teas: Map<String, Tea>
): List<TopRatedTea> =
    completedIn(window).filter { it.effectiveRating != null }
        .groupBy { it.session.teaId }
        .mapNotNull { (teaId, sessions) ->
            teas[teaId]?.let { tea ->
                TopRatedTea(
                    tea,
                    sessions.mapNotNull { it.effectiveRating }.average().toFloat(),
                    sessions.size
                )
            }
        }
        .sortedByDescending { it.averageRating }
        .take(5)
