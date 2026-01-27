package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock

class CompleteSessionUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(
        session: TeaSession,
        rating: Float? = null,
        finalNotes: String? = null
    ): Result<TeaSession> {
        if (session.status == SessionStatus.COMPLETED) {
            return Result.failure(IllegalStateException("Session is already completed"))
        }

        if (rating != null && (rating !in 0f..5f)) {
            return Result.failure(IllegalArgumentException("Rating must be between 0 and 5"))
        }

        // Update session to completed
        val completedSession = session.copy(
            status = SessionStatus.COMPLETED,
            rating = rating,
            notes = finalNotes?.takeIf { it.isNotBlank() }?.trim() ?: session.notes,
            updatedAt = Clock.System.now()
        )

        return try {
            teaSessionRepository.upsert(completedSession)

            // Update tea statistics
            updateTeaStats(session.teaId)

            Result.success(completedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateTeaStats(teaId: String) {
        try {
            // Get all completed sessions for this tea
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
                    lastBrewedAt = lastBrewedAt
                )?.let { teaRepository.upsert(it) }
                ?: throw IllegalStateException("Tea with id $teaId not found")
        } catch (e: Exception) {
            // Non-critical - stats update failed but session was saved
            println("Failed to update tea stats: ${e.message}")
        }
    }
}