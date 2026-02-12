package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TrendPoint
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class GetBrewingTrendsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): List<TrendPoint> {
        val tz = TimeZone.currentSystemDefault()
        val sessions = teaSessionRepository.getByDateRange(start, end)

        val completedParents = sessions.filter {
            it.status == SessionStatus.COMPLETED && it.parentSessionId == null
        }

        if (completedParents.isEmpty()) return emptyList()

        val countsByDate = completedParents.groupBy { session ->
            session.timestamp.toLocalDateTime(tz).date
        }.mapValues { (_, sessions) -> sessions.size }

        // Use earliest session date as start bound to avoid iterating millions of days
        // when period is "All Time" (Instant.DISTANT_PAST)
        val earliestSessionDate = countsByDate.keys.min()
        val requestedStartDate = start.toLocalDateTime(tz).date
        val startDate = maxOf(requestedStartDate, earliestSessionDate)
        val endDate = end.toLocalDateTime(tz).date

        val result = mutableListOf<TrendPoint>()
        var current = startDate
        while (current <= endDate) {
            result.add(TrendPoint(date = current, sessionCount = countsByDate[current] ?: 0))
            current = current.plus(1, DateTimeUnit.DAY)
        }

        return result
    }
}
