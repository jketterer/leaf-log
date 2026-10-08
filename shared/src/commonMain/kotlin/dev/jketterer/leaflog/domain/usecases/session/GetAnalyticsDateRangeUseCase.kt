package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.startOfWeek
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class GetAnalyticsDateRangeUseCase {
    /** The `[start, end)` window [period] covers at [now] in [tz]. */
    operator fun invoke(
        period: AnalyticsPeriod,
        customRange: Pair<LocalDate, LocalDate>?,
        now: Instant,
        tz: TimeZone,
    ): Pair<Instant, Instant> {
        val today = now.toLocalDateTime(tz).date

        return when (period) {
            AnalyticsPeriod.LAST_7_DAYS -> {
                val start = today.minus(6, DateTimeUnit.DAY)
                start.atStartOfDayIn(tz) to now
            }

            AnalyticsPeriod.LAST_30_DAYS -> {
                val start = today.minus(30, DateTimeUnit.DAY)
                start.atStartOfDayIn(tz) to now
            }

            AnalyticsPeriod.LAST_90_DAYS -> {
                val start = today.minus(90, DateTimeUnit.DAY)
                start.atStartOfDayIn(tz) to now
            }

            AnalyticsPeriod.THIS_WEEK -> {
                val startOfWeek = today.startOfWeek()
                val endOfWeek = startOfWeek.plus(7, DateTimeUnit.DAY)
                startOfWeek.atStartOfDayIn(tz) to endOfWeek.atStartOfDayIn(tz)
            }

            AnalyticsPeriod.THIS_MONTH -> {
                val firstOfMonth = LocalDate(today.year, today.month, 1)
                val firstOfNextMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH)
                firstOfMonth.atStartOfDayIn(tz) to firstOfNextMonth.atStartOfDayIn(tz)
            }

            AnalyticsPeriod.LAST_MONTH -> {
                val firstOfThisMonth = LocalDate(today.year, today.month, 1)
                val firstOfLastMonth = firstOfThisMonth.minus(1, DateTimeUnit.MONTH)
                firstOfLastMonth.atStartOfDayIn(tz) to firstOfThisMonth.atStartOfDayIn(tz)
            }

            AnalyticsPeriod.THIS_YEAR -> {
                val firstOfYear = LocalDate(today.year, 1, 1)
                firstOfYear.atStartOfDayIn(tz) to now
            }

            AnalyticsPeriod.ALL_TIME -> {
                Instant.DISTANT_PAST to now
            }

            AnalyticsPeriod.CUSTOM -> {
                if (customRange != null) {
                    val (start, end) = customRange
                    start.atStartOfDayIn(tz) to end.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
                } else {
                    // Fallback to this month
                    val firstOfMonth = LocalDate(today.year, today.month, 1)
                    firstOfMonth.atStartOfDayIn(tz) to now
                }
            }
        }
    }
}
