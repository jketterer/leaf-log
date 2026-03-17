package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository

/**
 * Deletes a brewing configuration
 */
class DeleteBrewingConfigurationUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
) {
    suspend operator fun invoke(configurationId: String): Result<Unit> {
        return try {
            brewingConfigurationRepository.delete(configurationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
