package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** A meteorological season: the three months from [start], the first of Dec, Mar, Jun or Sep. */
data class Season(val start: LocalDate) {
    val lastMonth: LocalDate get() = start.plus(2, DateTimeUnit.MONTH)

    fun window(tz: TimeZone) = DateWindow(
        start.atStartOfDayIn(tz),
        start.plus(3, DateTimeUnit.MONTH).atStartOfDayIn(tz),
        tz,
    )

    /** December's season is winter in the north and summer in the south. */
    fun name(hemisphere: Hemisphere): SeasonName {
        val northern = SeasonName.entries[(start.month.ordinal + 1) % 12 / 3]
        return when (hemisphere) {
            Hemisphere.NORTHERN -> northern
            Hemisphere.SOUTHERN -> SeasonName.entries[(northern.ordinal + 2) % 4]
        }
    }

    companion object {
        fun of(date: LocalDate) = Season(date.startOfSeason())
    }
}

/** In the order the northern hemisphere meets them from December. */
enum class SeasonName { WINTER, SPRING, SUMMER, AUTUMN }

/** The first day of this date's meteorological season. */
fun LocalDate.startOfSeason(): LocalDate =
    LocalDate(year, month, 1).minus((month.ordinal + 1) % 3, DateTimeUnit.MONTH)

/** Seasons ended by [now] with 10 or more completed sessions, newest first. */
fun List<SessionWithSteeps>.seasonRecaps(now: Instant, tz: TimeZone): List<Season> {
    val current = Season.of(now.toLocalDateTime(tz).date)
    return filter { it.session.status == SessionStatus.COMPLETED }
        .groupingBy { Season.of(it.startedAt.toLocalDateTime(tz).date) }.eachCount()
        .filter { (season, count) -> season.start < current.start && count >= 10 }
        .keys.sortedByDescending { it.start }
}
