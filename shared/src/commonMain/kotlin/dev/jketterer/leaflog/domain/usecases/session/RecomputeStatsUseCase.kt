package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.stats
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

/**
 * Rebuilds what a tea and its brewing configurations store about their sessions. Every session
 * write runs this inside its transaction, so a failure here rolls the write back.
 */
class RecomputeStatsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
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

        // A configuration is for one tea, so every session that used it is among these
        brewingConfigurationRepository.getByTeaId(teaId).forEach { configuration ->
            val used = sessions.filter { it.session.usedConfigurationId == configuration.id }.stats()
            brewingConfigurationRepository.upsert(
                configuration.copy(timesUsed = used.count, lastUsedAt = used.lastAt),
            )
        }
    }
}
