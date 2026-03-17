package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlin.math.abs

/**
 * Returns true if an identical brewing configuration already exists for the session's
 * tea + vessel combination, so the save dialog can be suppressed.
 *
 * "Identical" means within measurement tolerance on all brewing parameters:
 *  - temperature  ±1 °C
 *  - water volume ±10 ml
 *  - brewing time ±5 s
 *  - tea quantity ±0.5 g (or both absent)
 */
class CheckDuplicateConfigurationUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
) {
    suspend operator fun invoke(session: TeaSession): Boolean {
        val existing = brewingConfigurationRepository.getByTeaAndVessel(session.teaId, session.vesselId)
        return existing.any { it.isDuplicateOf(session) }
    }

    private fun BrewingConfiguration.isDuplicateOf(session: TeaSession): Boolean {
        val quantityMatch = when {
            teaQuantityGrams == null && session.teaQuantityGrams == null -> true
            teaQuantityGrams != null && session.teaQuantityGrams != null ->
                abs(teaQuantityGrams - session.teaQuantityGrams) <= 0.5f
            else -> false
        }
        return quantityMatch &&
            abs(waterQuantityMl - session.waterQuantityMl) <= 10.0 &&
            abs(temperatureCelsius - session.temperatureCelsius) <= 1.0 &&
            abs(brewingTime.inWholeSeconds - session.brewingTime.inWholeSeconds) <= 5
    }
}
