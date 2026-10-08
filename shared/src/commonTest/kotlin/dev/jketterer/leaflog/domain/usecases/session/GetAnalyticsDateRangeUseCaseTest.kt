package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.AnalyticsDateRange
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAnalyticsDateRangeUseCaseTest {

    private val getDateRange = GetAnalyticsDateRangeUseCase()
    private val tz = TimeZone.of("America/New_York")

    private fun at(dateTime: String) = LocalDateTime.parse(dateTime).toInstant(tz)

    private fun midnight(date: String) = LocalDate.parse(date).atStartOfDayIn(tz)

    private fun rangeAt(period: AnalyticsPeriod, now: String): AnalyticsDateRange =
        getDateRange(period = period, customRange = null, now = at(now), tz = tz)

    @Test
    fun `this week on a Sunday runs Monday to Monday and includes today`() {
        val range = rangeAt(AnalyticsPeriod.THIS_WEEK, now = "2026-09-27T20:00")

        assertEquals(midnight("2026-09-21"), range.start)
        assertEquals(midnight("2026-09-28"), range.end)
    }

    @Test
    fun `this week on a Monday starts today`() {
        val range = rangeAt(AnalyticsPeriod.THIS_WEEK, now = "2026-09-28T08:00")

        assertEquals(midnight("2026-09-28"), range.start)
        assertEquals(midnight("2026-10-05"), range.end)
    }

    @Test
    fun `this week is compared against the same elapsed part of last week`() {
        val range = rangeAt(AnalyticsPeriod.THIS_WEEK, now = "2026-09-23T14:30")

        assertEquals(midnight("2026-09-14"), range.previousStart)
        assertEquals(at("2026-09-16T14:30"), range.previousEnd)
    }

    @Test
    fun `this month is compared against the same elapsed part of last month`() {
        val range = rangeAt(AnalyticsPeriod.THIS_MONTH, now = "2026-09-23T14:30")

        assertEquals(midnight("2026-08-01"), range.previousStart)
        assertEquals(at("2026-08-23T14:30"), range.previousEnd)
    }

    @Test
    fun `this year is compared against the same elapsed part of last year`() {
        val range = rangeAt(AnalyticsPeriod.THIS_YEAR, now = "2026-09-23T14:30")

        assertEquals(midnight("2025-01-01"), range.previousStart)
        assertEquals(at("2025-09-23T14:30"), range.previousEnd)
    }

    @Test
    fun `a rolling window is compared against the window just before it`() {
        val range = rangeAt(AnalyticsPeriod.LAST_7_DAYS, now = "2026-09-23T14:30")

        assertEquals(range.start, range.previousEnd)
        assertEquals(range.end - range.start, range.previousEnd - range.previousStart)
    }
}
