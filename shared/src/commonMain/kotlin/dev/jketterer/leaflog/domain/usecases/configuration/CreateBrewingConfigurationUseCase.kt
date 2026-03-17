package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Creates a brewing configuration manually (not from a session)
 */
class CreateBrewingConfigurationUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val generateConfigurationLabelUseCase: GenerateConfigurationLabelUseCase,
) {
    /**
     * Create a configuration manually
     *
     * @param teaId The tea this configuration is for
     * @param vesselId The brewing vessel to use
     * @param teaQuantityGrams Optional tea quantity in grams
     * @param waterQuantityMl Water quantity in milliliters
     * @param temperatureCelsius Brewing temperature in Celsius
     * @param brewingTime Brewing duration
     * @param waterType Type of water used
     * @param customLabel Optional custom label (auto-generated if null)
     * @return Result with the created configuration
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        teaId: String,
        vesselId: String,
        teaQuantityGrams: Float?,
        waterQuantityMl: Double,
        temperatureCelsius: Double,
        brewingTime: Duration,
        waterType: WaterType,
        customLabel: String? = null,
    ): Result<BrewingConfiguration> {
        return try {
            val label = customLabel?.takeIf { it.isNotBlank() }
                ?: generateConfigurationLabelUseCase(
                    teaQuantityGrams = teaQuantityGrams,
                    waterQuantityMl = waterQuantityMl,
                    brewingTime = brewingTime,
                )

            val now = Clock.System.now()
            val configuration = BrewingConfiguration(
                id = Uuid.random().toString(),
                teaId = teaId,
                vesselId = vesselId,
                teaQuantityGrams = teaQuantityGrams,
                waterQuantityMl = waterQuantityMl,
                temperatureCelsius = temperatureCelsius,
                brewingTime = brewingTime,
                waterType = waterType,
                sourceSessionId = null,
                rating = null,
                timesUsed = 0,
                lastUsedAt = null,
                label = label,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )

            brewingConfigurationRepository.upsert(configuration)
            Result.success(configuration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
