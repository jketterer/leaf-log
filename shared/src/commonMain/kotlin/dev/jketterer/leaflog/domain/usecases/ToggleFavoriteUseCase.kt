package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import kotlin.time.Clock

class ToggleFavoriteUseCase(
    private val teaRepository: TeaRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(tea: Tea): Result<Tea> {
        val updatedTea = tea.copy(
            isFavorite = !tea.isFavorite,
            updatedAt = clock.now()
        )

        return try {
            teaRepository.upsert(updatedTea)
            Result.success(updatedTea)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}