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
        val sessions = teaSessionRepository.getSessionsStartedBetween(start, end)
            .filter { it.session.status == SessionStatus.COMPLETED }
        val steeps = sessions.flatMap { it.steeps }

        val totalBrewingTime = steeps.fold(Duration.ZERO) { acc, steep -> acc + steep.brewingTime }

        // Water is per steep, each one fresh. Leaf is per session: every steep re-uses it.
        val totalWaterMl = steeps.sumOf { it.waterQuantityMl }
        val weighedSessions = sessions.mapNotNull { it.session.teaQuantityGrams }
        val totalTeaGrams = weighedSessions.sumOf { it.toDouble() }

        val ratings = sessions.mapNotNull { it.effectiveRating }

        return AnalyticsData(
            totalSessions = sessions.size,
            totalBrewingTime = totalBrewingTime,
            totalWaterMl = totalWaterMl,
            uniqueTeasCount = sessions.map { it.session.teaId }.distinct().size,
            averageRating = ratings.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
            ratedSessionsCount = ratings.size,
            totalTeaGrams = totalTeaGrams,
            weighedSessionsCount = weighedSessions.size,
        )
    }
}
