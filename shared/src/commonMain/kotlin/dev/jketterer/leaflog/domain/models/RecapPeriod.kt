package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The whole months a recap tells, a season or a calendar year. */
sealed interface RecapPeriod {
    val start: LocalDate

    /** The first day of its last month. */
    val lastMonth: LocalDate

    /** The one before it, which its flavors are compared with. */
    val previous: RecapPeriod

    /** The day after it ends. */
    val end: LocalDate get() = lastMonth.plus(1, DateTimeUnit.MONTH)

    fun window(tz: TimeZone) = DateWindow(start.atStartOfDayIn(tz), end.atStartOfDayIn(tz), tz)
}

/** A meteorological season: the three months from [start], the first of Dec, Mar, Jun or Sep. */
data class Season(override val start: LocalDate) : RecapPeriod {
    override val lastMonth: LocalDate get() = start.plus(2, DateTimeUnit.MONTH)

    override val previous: Season get() = Season(start.minus(3, DateTimeUnit.MONTH))

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

data class CalendarYear(val year: Int) : RecapPeriod {
    override val start: LocalDate get() = LocalDate(year, Month.JANUARY, 1)

    override val lastMonth: LocalDate get() = LocalDate(year, Month.DECEMBER, 1)

    override val previous: CalendarYear get() = CalendarYear(year - 1)
}

/** In the order the northern hemisphere meets them from December. */
enum class SeasonName { WINTER, SPRING, SUMMER, AUTUMN }

/** The first day of this date's meteorological season. */
fun LocalDate.startOfSeason(): LocalDate =
    LocalDate(year, month, 1).minus((month.ordinal + 1) % 3, DateTimeUnit.MONTH)

/**
 * Recaps ended by [now], the newest ending first: seasons with 10 or more completed sessions, and
 * years with any. A year needs the first completed session by its March, so a year begun in June
 * isn't called your year.
 */
fun List<SessionWithSteeps>.recaps(now: Instant, tz: TimeZone): List<RecapPeriod> {
    val today = now.toLocalDateTime(tz).date
    val dates = filter { it.session.status == SessionStatus.COMPLETED }
        .map { it.startedAt.toLocalDateTime(tz).date }
    val first = dates.minOrNull() ?: return emptyList()
    val firstYear = if (first.month <= Month.MARCH) first.year else first.year + 1
    val seasons = dates.groupingBy { Season.of(it) }.eachCount()
        .filter { (_, count) -> count >= 10 }.keys
    val years = dates.map { it.year }.distinct().filter { it >= firstYear }.map { CalendarYear(it) }
    return (seasons + years).filter { it.end <= today }.sortedByDescending { it.end }
}
