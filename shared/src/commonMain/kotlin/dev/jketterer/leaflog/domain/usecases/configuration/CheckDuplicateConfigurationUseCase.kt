package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlin.math.abs

/**
 * Returns true if an identical brewing configuration already exists for the session's
 * tea + vessel combination, so the save dialog can be suppressed.
 *
 * "Identical" means within measurement tolerance on all brewing parameters:
 *  - temperature  ±1 °C
 *  - water volume ±10 ml
 *  - base brewing time ±5 s, and the same increment and overrides
 *  - tea quantity ±0.5 g (or both absent)
 */
class CheckDuplicateConfigurationUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
) {
    suspend operator fun invoke(session: SessionWithSteeps): Boolean {
        val existing = brewingConfigurationRepository.getByTeaAndVessel(
            session.session.teaId,
            session.session.vesselId,
        )
        return existing.any { it.isDuplicateOf(session) }
    }

    // A method holds the first steep's parameters and the plan the steeps followed
    private fun BrewingConfiguration.isDuplicateOf(session: SessionWithSteeps): Boolean {
        val leaf = session.session.teaQuantityGrams
        val steep = session.steeps.first()
        val learned = BrewingPlan.learnedFrom(session.steeps)
        val quantityMatch = when {
            teaQuantityGrams == null && leaf == null -> true
            teaQuantityGrams != null && leaf != null -> abs(teaQuantityGrams - leaf) <= 0.5f
            else -> false
        }
        return quantityMatch &&
            abs(waterQuantityMl - steep.waterQuantityMl) <= 10.0 &&
            abs(temperatureCelsius - steep.temperatureCelsius) <= 1.0 &&
            abs(brewingTime.inWholeSeconds - learned.base.inWholeSeconds) <= 5 &&
            steepIncrement == learned.increment &&
            steepOverrides == learned.overrides
    }
}
