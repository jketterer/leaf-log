package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlin.time.Clock
import kotlin.time.Duration
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
     * @param brewingTime Brewing duration of the first steep
     * @param steepIncrement Time added to each further steep, null for a single western steep
     * @param waterType Type of water used
     * @param rinse Whether sessions from it start rinsed
     * @param customLabel Optional custom label (auto-generated if null)
     * @return Result with the created configuration
     */
    suspend operator fun invoke(
        teaId: String,
        vesselId: String,
        teaQuantityGrams: Float?,
        waterQuantityMl: Double,
        temperatureCelsius: Double,
        brewingTime: Duration,
        steepIncrement: Duration? = null,
        waterType: WaterType,
        rinse: Boolean = false,
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
                steepIncrement = steepIncrement,
                waterType = waterType,
                rinse = rinse,
                sourceSessionId = null,
                rating = null,
                timesUsed = 0,
                lastUsedAt = null,
                label = label,
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
