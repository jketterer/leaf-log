package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlin.time.Clock

/**
 * Updates an existing brewing configuration
 */
class UpdateBrewingConfigurationUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val clock: Clock,
) {
    /**
     * Update a configuration's parameters
     *
     * @param configurationId ID of the configuration to update
     * @param label Optional new label
     * @param teaQuantityGrams Optional new tea quantity
     * @param waterQuantityMl Optional new water quantity
     * @param temperatureCelsius Optional new temperature
     * @param plan Optional new plan; replaces the base time, increment and overrides together, so a
     *   null increment or empty overrides in it clear them
     * @param waterType Optional new water type
     * @param rinse Optional new rinse
     * @return Result with the updated configuration
     */
    suspend operator fun invoke(
        configurationId: String,
        label: String? = null,
        vesselId: String? = null,
        teaQuantityGrams: Float? = null,
        waterQuantityMl: Double? = null,
        temperatureCelsius: Double? = null,
        plan: BrewingPlan? = null,
        waterType: WaterType? = null,
        rinse: Boolean? = null,
    ): Result<BrewingConfiguration> {
        return try {
            val existing = brewingConfigurationRepository.getById(configurationId)
                ?: return Result.failure(IllegalArgumentException("Configuration not found"))

            val updated = existing.copy(
                label = label ?: existing.label,
                vesselId = vesselId ?: existing.vesselId,
                teaQuantityGrams = teaQuantityGrams ?: existing.teaQuantityGrams,
                waterQuantityMl = waterQuantityMl ?: existing.waterQuantityMl,
                temperatureCelsius = temperatureCelsius ?: existing.temperatureCelsius,
                brewingTime = plan?.base ?: existing.brewingTime,
                steepIncrement = if (plan != null) plan.increment else existing.steepIncrement,
                steepOverrides = plan?.overrides ?: existing.steepOverrides,
                waterType = waterType ?: existing.waterType,
                rinse = rinse ?: existing.rinse,
                updatedAt = clock.now(),
            )

            brewingConfigurationRepository.upsert(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
