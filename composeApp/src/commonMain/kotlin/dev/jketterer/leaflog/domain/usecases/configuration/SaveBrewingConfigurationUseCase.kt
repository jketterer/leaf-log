package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlin.time.Clock

/**
 * Saves a brewing configuration from a successful tea session
 */
class SaveBrewingConfigurationUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val generateConfigurationLabelUseCase: GenerateConfigurationLabelUseCase,
) {
    /**
     * Save a configuration from a session
     *
     * @param session The session to save as a configuration
     * @param customLabel Optional custom label (if null, auto-generates)
     * @return Result with the saved configuration
     */
    suspend operator fun invoke(
        session: TeaSession,
        customLabel: String? = null,
    ): Result<BrewingConfiguration> {
        return try {
            // Validate that session has required data
            if (session.rating == null || session.rating < 3f) {
                return Result.failure(IllegalArgumentException("Session must have a rating of 3+ stars"))
            }

            // Generate label if not provided
            val label = customLabel ?: generateConfigurationLabelUseCase(
                teaQuantityGrams = session.teaQuantityGrams,
                waterQuantityMl = session.waterQuantityMl,
                brewingTime = session.brewingTime,
            )

            val now = Clock.System.now()
            val configuration = BrewingConfiguration(
                id = generateId(),
                teaId = session.teaId,
                vesselId = session.vesselId,
                teaQuantityGrams = session.teaQuantityGrams,
                waterQuantityMl = session.waterQuantityMl,
                temperatureCelsius = session.temperatureCelsius,
                brewingTime = session.brewingTime,
                waterType = session.waterType,
                sourceSessionId = session.id,
                rating = session.rating,
                timesUsed = 1,
                lastUsedAt = now,
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

    private fun generateId(): String {
        return "config_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
    }
}
