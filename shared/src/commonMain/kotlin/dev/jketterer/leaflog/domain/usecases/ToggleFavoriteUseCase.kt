package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import kotlin.time.Clock

class ToggleFavoriteUseCase(
    private val teaRepository: TeaRepository,
) {
    suspend operator fun invoke(tea: Tea): Result<Tea> {
        val updatedTea = tea.copy(
            isFavorite = !tea.isFavorite,
            updatedAt = Clock.System.now()
        )

        return try {
            teaRepository.upsert(updatedTea)
            Result.success(updatedTea)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}