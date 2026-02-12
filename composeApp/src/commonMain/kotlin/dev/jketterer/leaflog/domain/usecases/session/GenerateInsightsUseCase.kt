package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.AnalyticsData
import dev.jketterer.leaflog.domain.models.Insight
import dev.jketterer.leaflog.domain.models.InsightType
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Instant

class GenerateInsightsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(
        current: AnalyticsData,
        previous: AnalyticsData?,
        teas: List<Tea>,
        periodLabel: String,
        start: Instant,
        end: Instant,
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // Period comparison
        if (previous != null && previous.totalSessions > 0) {
            val change = ((current.totalSessions - previous.totalSessions).toFloat() /
                    previous.totalSessions * 100f)
            if (change.absoluteValue > 10f) {
                val direction = if (change > 0) "up" else "down"
                val absChange = change.absoluteValue.roundToInt()
                insights.add(
                    Insight(
                        type = InsightType.PERIOD_COMPARISON,
                        text = "Sessions are $direction ${absChange}% compared to the previous period",
                    )
                )
            }
        }

        // Most brewed tea
        if (current.totalSessions > 0) {
            val sessions = teaSessionRepository.getByDateRange(start, end)
                .filter { it.status == SessionStatus.COMPLETED && it.parentSessionId == null }
            val teaCountMap = sessions.groupBy { it.teaId }
                .mapValues { it.value.size }
            val topTeaId = teaCountMap.maxByOrNull { it.value }?.key
            val topTea = teas.find { it.id == topTeaId }
            val topCount = teaCountMap[topTeaId] ?: 0
            if (topTea != null && topCount > 1) {
                insights.add(
                    Insight(
                        type = InsightType.MOST_BREWED,
                        text = "${topTea.name} is your most brewed tea with $topCount sessions",
                    )
                )
            }
        }

        // Average rating
        if (current.ratedSessionsCount >= 5 && current.averageRating != null) {
            val rounded = (current.averageRating * 10).roundToInt() / 10f
            insights.add(
                Insight(
                    type = InsightType.AVERAGE_RATING,
                    text = "Your average rating is $rounded across ${current.ratedSessionsCount} rated sessions",
                )
            )
        }

        // Variety
        if (current.uniqueTeasCount > 1) {
            insights.add(
                Insight(
                    type = InsightType.VARIETY,
                    text = "You've explored ${current.uniqueTeasCount} different teas $periodLabel",
                )
            )
        }

        return insights.take(4)
    }
}
