package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

class DeleteSessionUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> {
        if (sessionId.isBlank()) {
            return Result.failure(IllegalArgumentException("Session ID cannot be empty"))
        }

        return try {
            teaSessionRepository.delete(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}