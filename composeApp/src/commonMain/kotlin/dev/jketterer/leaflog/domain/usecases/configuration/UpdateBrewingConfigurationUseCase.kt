package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Updates an existing brewing configuration
 */
class UpdateBrewingConfigurationUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
) {
    /**
     * Update a configuration's parameters
     *
     * @param configurationId ID of the configuration to update
     * @param label Optional new label
     * @param teaQuantityGrams Optional new tea quantity
     * @param waterQuantityMl Optional new water quantity
     * @param temperatureCelsius Optional new temperature
     * @param brewingTime Optional new brewing time
     * @param waterType Optional new water type
     * @param isActive Optional new active status
     * @return Result with the updated configuration
     */
    suspend operator fun invoke(
        configurationId: String,
        label: String? = null,
        teaQuantityGrams: Float? = null,
        waterQuantityMl: Double? = null,
        temperatureCelsius: Double? = null,
        brewingTime: Duration? = null,
        waterType: WaterType? = null,
        isActive: Boolean? = null,
    ): Result<BrewingConfiguration> {
        return try {
            val existing = brewingConfigurationRepository.getById(configurationId)
                ?: return Result.failure(IllegalArgumentException("Configuration not found"))

            val updated = existing.copy(
                label = label ?: existing.label,
                teaQuantityGrams = teaQuantityGrams ?: existing.teaQuantityGrams,
                waterQuantityMl = waterQuantityMl ?: existing.waterQuantityMl,
                temperatureCelsius = temperatureCelsius ?: existing.temperatureCelsius,
                brewingTime = brewingTime ?: existing.brewingTime,
                waterType = waterType ?: existing.waterType,
                isActive = isActive ?: existing.isActive,
                updatedAt = Clock.System.now(),
            )

            brewingConfigurationRepository.upsert(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
