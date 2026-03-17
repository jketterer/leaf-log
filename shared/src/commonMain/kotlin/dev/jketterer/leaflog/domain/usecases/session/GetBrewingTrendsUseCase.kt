package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingTrends
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TrendGranularity
import dev.jketterer.leaflog.domain.models.TrendPoint
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class GetBrewingTrendsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): BrewingTrends {
        val tz = TimeZone.currentSystemDefault()
        val sessions = teaSessionRepository.getByDateRange(start, end)

        val completedParents = sessions.filter {
            it.status == SessionStatus.COMPLETED && it.parentSessionId == null
        }

        if (completedParents.isEmpty()) return BrewingTrends(emptyList(), TrendGranularity.DAILY)

        val countsByDate = completedParents.groupBy { session ->
            session.timestamp.toLocalDateTime(tz).date
        }.mapValues { (_, sessions) -> sessions.size }

        // Use earliest session date as start bound to avoid iterating millions of days
        // when period is "All Time" (Instant.DISTANT_PAST)
        val earliestSessionDate = countsByDate.keys.min()
        val requestedStartDate = start.toLocalDateTime(tz).date
        val startDate = maxOf(requestedStartDate, earliestSessionDate)
        val endDate = end.toLocalDateTime(tz).date

        val daySpan = startDate.daysUntil(endDate)
        val granularity = when {
            daySpan <= 45 -> TrendGranularity.DAILY
            daySpan <= 180 -> TrendGranularity.WEEKLY
            else -> TrendGranularity.MONTHLY
        }

        val points = when (granularity) {
            TrendGranularity.DAILY -> buildDailyPoints(startDate, endDate, countsByDate)
            TrendGranularity.WEEKLY -> buildWeeklyPoints(startDate, endDate, countsByDate)
            TrendGranularity.MONTHLY -> buildMonthlyPoints(startDate, endDate, countsByDate)
        }

        return BrewingTrends(points, granularity)
    }

    private fun buildDailyPoints(
        startDate: LocalDate,
        endDate: LocalDate,
        countsByDate: Map<LocalDate, Int>,
    ): List<TrendPoint> {
        val result = mutableListOf<TrendPoint>()
        var current = startDate
        while (current <= endDate) {
            result.add(TrendPoint(date = current, sessionCount = countsByDate[current] ?: 0))
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return result
    }

    private fun buildWeeklyPoints(
        startDate: LocalDate,
        endDate: LocalDate,
        countsByDate: Map<LocalDate, Int>,
    ): List<TrendPoint> {
        // Bucket daily counts by week start (Monday)
        val weekCounts = mutableMapOf<LocalDate, Int>()
        countsByDate.forEach { (date, count) ->
            val monday = date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY)
            weekCounts[monday] = (weekCounts[monday] ?: 0) + count
        }

        // Generate points for all weeks in range
        val result = mutableListOf<TrendPoint>()
        var weekStart = startDate.minus(startDate.dayOfWeek.ordinal, DateTimeUnit.DAY)
        while (weekStart <= endDate) {
            result.add(TrendPoint(date = weekStart, sessionCount = weekCounts[weekStart] ?: 0))
            weekStart = weekStart.plus(7, DateTimeUnit.DAY)
        }
        return result
    }

    private fun buildMonthlyPoints(
        startDate: LocalDate,
        endDate: LocalDate,
        countsByDate: Map<LocalDate, Int>,
    ): List<TrendPoint> {
        // Bucket daily counts by month start (1st of month)
        val monthCounts = mutableMapOf<LocalDate, Int>()
        countsByDate.forEach { (date, count) ->
            val monthStart = LocalDate(date.year, date.month, 1)
            monthCounts[monthStart] = (monthCounts[monthStart] ?: 0) + count
        }

        // Generate points for all months in range
        val result = mutableListOf<TrendPoint>()
        var monthStart = LocalDate(startDate.year, startDate.month, 1)
        while (monthStart <= endDate) {
            result.add(TrendPoint(date = monthStart, sessionCount = monthCounts[monthStart] ?: 0))
            monthStart = monthStart.plus(1, DateTimeUnit.MONTH)
        }
        return result
    }
}
