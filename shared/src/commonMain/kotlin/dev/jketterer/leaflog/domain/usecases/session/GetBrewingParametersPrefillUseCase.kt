package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository

/**
 * Use case to pre-fill brewing parameters from what has worked before.
 *
 * Priority order, from most to least specific to this exact cup:
 * 1. Saved brewing configuration for this exact tea + vessel (an explicit choice, so it wins)
 * 2. The highest-rated past session of this tea + vessel
 * 3. Saved brewing configuration for another tea of the same type + vessel
 * 4. This tea's own default temperature
 * 5. Its tea type's default temperature
 * 6. No pre-fill
 *
 * Steps 1-3 carry a whole method; 4 and 5 only a temperature. See [isCompleteMethod] - callers
 * infer things from a pre-fill having happened, and a bare temperature must not read as a method.
 */
class GetBrewingParametersPrefillUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(tea: Tea, vessel: BrewingVessel): BrewingParametersPrefill {
        // 1. Saved brewing configuration for this exact tea + vessel
        val exactConfig = brewingConfigurationRepository.getBestByTeaAndVessel(tea.id, vessel.id)
        if (exactConfig != null) {
            return BrewingParametersPrefill(
                teaQuantityGrams = exactConfig.teaQuantityGrams,
                waterQuantityMl = exactConfig.waterQuantityMl,
                temperatureCelsius = exactConfig.temperatureCelsius,
                brewingTime = exactConfig.brewingTime,
                waterType = exactConfig.waterType,
                source = PrefillSource.SavedConfig,
                rating = exactConfig.rating,
            )
        }

        // 2. The best brew of this tea you have already had in this vessel
        val bestSession = findBestRatedSession(tea.id, vessel.id)
        if (bestSession != null) {
            return BrewingParametersPrefill(
                teaQuantityGrams = bestSession.teaQuantityGrams,
                waterQuantityMl = bestSession.waterQuantityMl,
                temperatureCelsius = bestSession.temperatureCelsius,
                brewingTime = bestSession.brewingTime,
                waterType = bestSession.waterType,
                source = PrefillSource.BestRatedSession(bestSession.effectiveRating()!!),
                rating = bestSession.effectiveRating(),
            )
        }

        val teaType = teaTypeRepository.getById(tea.teaTypeId)

        // 3. Saved brewing configuration for another tea of the same type + this vessel
        if (teaType != null) {
            val sameTypeTeas = teaRepository.getAll()
                .filter { it.teaTypeId == tea.teaTypeId && it.id != tea.id }
            for (otherTea in sameTypeTeas) {
                val typeConfig =
                    brewingConfigurationRepository.getBestByTeaAndVessel(otherTea.id, vessel.id)
                if (typeConfig != null) {
                    return BrewingParametersPrefill(
                        teaQuantityGrams = typeConfig.teaQuantityGrams,
                        waterQuantityMl = typeConfig.waterQuantityMl,
                        temperatureCelsius = typeConfig.temperatureCelsius,
                        brewingTime = typeConfig.brewingTime,
                        waterType = typeConfig.waterType,
                        source = PrefillSource.SameTypeConfig(teaName = otherTea.name),
                        rating = typeConfig.rating,
                    )
                }
            }
        }

        // 4./5. A default temperature, from the tea or failing that its type. Temperature only:
        // nothing here knows a brewing time or a quantity, so the rest stays null.
        val default = tea.defaultTemperatureCelsius?.let { it to PrefillSource.TeaDefault }
            ?: teaType?.defaultTemperatureCelsius?.let {
                it to PrefillSource.TypeDefault(teaType.name)
            }
        if (default != null) {
            val (temperature, source) = default
            return temperatureOnlyPrefill(temperature, source)
        }

        // 6. No pre-fill available
        return temperatureOnlyPrefill(temperature = null, source = PrefillSource.None)
    }

    private fun temperatureOnlyPrefill(temperature: Double?, source: PrefillSource) =
        BrewingParametersPrefill(
            teaQuantityGrams = null,
            waterQuantityMl = null,
            temperatureCelsius = temperature,
            brewingTime = null,
            waterType = null,
            rating = null,
            source = source,
        )

    /**
     * The best completed brew of this tea in this vessel, or null if none clears [MINIMUM_RATING].
     *
     * Only parent sessions are considered: their parameters describe the first steep, which is
     * what a new session is about to start. Brews rated below the floor are skipped rather than
     * suggested back, since reproducing a cup you did not enjoy is worse than a generic starting
     * point from the same tea type.
     */
    private suspend fun findBestRatedSession(teaId: String, vesselId: String): TeaSession? {
        return teaSessionRepository.getByTeaId(teaId)
            .filter {
                it.status == SessionStatus.COMPLETED &&
                    it.parentSessionId == null &&
                    it.vesselId == vesselId &&
                    (it.effectiveRating() ?: 0f) >= MINIMUM_RATING
            }
            .maxWithOrNull(
                compareBy<TeaSession> { it.effectiveRating() ?: 0f }.thenBy { it.timestamp },
            )
    }

    private companion object {
        /** Below this, a past brew is treated as a cup not worth repeating. */
        const val MINIMUM_RATING = 3.0f
    }
}

/** Rating for the brew as a whole, preferring the multi-steep average over a single steep. */
private fun TeaSession.effectiveRating(): Float? = averageRating ?: rating
