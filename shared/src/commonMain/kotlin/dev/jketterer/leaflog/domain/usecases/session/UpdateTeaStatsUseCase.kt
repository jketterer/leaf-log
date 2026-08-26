package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

class UpdateTeaStatsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(teaId: String) {
        try {
            val sessions = teaSessionRepository.getByTeaId(teaId)
                .filter { it.status == SessionStatus.COMPLETED && it.parentSessionId == null }

            val totalSessions = sessions.size
            val averageRating =
                sessions.mapNotNull { it.rating }.average().toFloat().takeIf { !it.isNaN() }
            val lastBrewedAt = sessions.maxOfOrNull { it.timestamp }

            teaRepository.getById(teaId)
                ?.copy(
                    totalSessions = totalSessions,
                    averageRating = averageRating,
                    lastBrewedAt = lastBrewedAt,
                )?.let { teaRepository.upsert(it) }
                ?: throw IllegalStateException("Tea with id $teaId not found")
        } catch (e: Exception) {
            // Non-critical - stats update failed but the primary operation was saved
            Logger.w(tag = "UpdateTeaStats") { "Failed to update tea stats: ${e.message}" }
        }
    }
}
