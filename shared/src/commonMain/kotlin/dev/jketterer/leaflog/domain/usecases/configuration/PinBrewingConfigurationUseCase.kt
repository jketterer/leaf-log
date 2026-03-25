package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository

/**
 * Pins or unpins a brewing configuration for the Quick Brew section.
 *
 * When pinning, the configuration is appended to the end of the pinned list by assigning
 * pinnedSortOrder = max(existing) + 1. When unpinning, the sort order is cleared.
 */
class PinBrewingConfigurationUseCase(
    private val repository: BrewingConfigurationRepository,
) {
    suspend operator fun invoke(configId: String, isPinned: Boolean): Result<Unit> =
        runCatching {
            if (isPinned) {
                val maxOrder = repository.getMaxPinnedSortOrder() ?: -1
                repository.setPinned(configId, isPinned = true, pinnedSortOrder = maxOrder + 1)
            } else {
                repository.setPinned(configId, isPinned = false, pinnedSortOrder = 0)
            }
        }
}
