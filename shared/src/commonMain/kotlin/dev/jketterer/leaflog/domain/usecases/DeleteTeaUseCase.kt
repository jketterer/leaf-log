package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.repositories.TeaRepository

class DeleteTeaUseCase(
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(teaId: String): Result<String> {
        if (teaId.isBlank()) {
            return Result.failure(IllegalArgumentException("Tea ID cannot be empty"))
        }

        return try {
            teaRepository.delete(teaId)
            Result.success(teaId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}