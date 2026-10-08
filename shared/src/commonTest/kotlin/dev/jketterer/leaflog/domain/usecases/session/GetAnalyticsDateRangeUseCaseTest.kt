package dev.jketterer.leaflog.domain.usecases.session

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

    private fun thisWeekAt(now: String) = getDateRange(
        period = AnalyticsPeriod.THIS_WEEK,
        customRange = null,
        now = LocalDateTime.parse(now).toInstant(tz),
        tz = tz,
    )

    private fun midnight(date: LocalDate) = date.atStartOfDayIn(tz)

    @Test
    fun `this week on a Sunday runs Monday to Monday and includes today`() {
        val (start, end) = thisWeekAt("2026-09-27T20:00")

        assertEquals(midnight(LocalDate(2026, 9, 21)), start)
        assertEquals(midnight(LocalDate(2026, 9, 28)), end)
    }

    @Test
    fun `this week on a Monday starts today`() {
        val (start, end) = thisWeekAt("2026-09-28T08:00")

        assertEquals(midnight(LocalDate(2026, 9, 28)), start)
        assertEquals(midnight(LocalDate(2026, 10, 5)), end)
    }
}
