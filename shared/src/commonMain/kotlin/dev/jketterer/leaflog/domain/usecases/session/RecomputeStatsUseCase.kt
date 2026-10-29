package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.stats
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

/**
 * Rebuilds what a tea stores about its sessions. Every session write runs this inside its
 * transaction, so a failure here rolls the write back.
 */
class RecomputeStatsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(teaId: String) {
        val sessions = teaSessionRepository.getSessionsByTea(teaId)
        val tea = teaRepository.getById(teaId) ?: throw DomainFailure.NotFound("tea $teaId")
        val teaStats = sessions.stats()
        teaRepository.upsert(
            tea.copy(
                totalSessions = teaStats.count,
                averageRating = teaStats.averageRating,
                lastBrewedAt = teaStats.lastAt,
            ),
        )
    }
}
