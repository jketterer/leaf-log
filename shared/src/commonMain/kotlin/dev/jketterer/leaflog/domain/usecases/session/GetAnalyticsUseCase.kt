package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.AnalyticsData
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Duration
import kotlin.time.Instant

class GetAnalyticsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): AnalyticsData {
        val allSessions = teaSessionRepository.getByDateRange(start, end)

        val completedSessions = allSessions.filter { it.status == SessionStatus.COMPLETED }
        val parentSessions = completedSessions.filter { it.parentSessionId == null }

        val totalBrewingTime = completedSessions.fold(Duration.ZERO) { acc, session ->
            acc + session.brewingTime
        }

        val totalWaterMl = completedSessions.sumOf { it.waterQuantityMl }

        val uniqueTeasCount = parentSessions.map { it.teaId }.distinct().size

        val ratedSessions = parentSessions.filter { it.rating != null || it.averageRating != null }
        val averageRating = if (ratedSessions.isNotEmpty()) {
            ratedSessions.map { it.averageRating ?: it.rating ?: 0f }.average().toFloat()
        } else {
            null
        }

        return AnalyticsData(
            totalSessions = parentSessions.size,
            totalBrewingTime = totalBrewingTime,
            totalWaterMl = totalWaterMl,
            uniqueTeasCount = uniqueTeasCount,
            averageRating = averageRating,
            ratedSessionsCount = ratedSessions.size,
        )
    }
}
