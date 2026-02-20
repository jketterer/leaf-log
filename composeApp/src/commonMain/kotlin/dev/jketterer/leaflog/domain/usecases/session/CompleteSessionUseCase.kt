package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock

class CompleteSessionUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val updateTeaStatsUseCase: UpdateTeaStatsUseCase,
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
            updateTeaStatsUseCase(session.teaId)
            Result.success(completedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
