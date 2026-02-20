package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

class DeleteSessionUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val updateTeaStatsUseCase: UpdateTeaStatsUseCase,
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> {
        if (sessionId.isBlank()) {
            return Result.failure(IllegalArgumentException("Session ID cannot be empty"))
        }

        return try {
            val session = teaSessionRepository.getById(sessionId)
            teaSessionRepository.delete(sessionId)
            session?.let { updateTeaStatsUseCase(it.teaId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}