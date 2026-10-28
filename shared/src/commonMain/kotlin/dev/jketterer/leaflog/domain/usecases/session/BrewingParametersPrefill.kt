package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

/**
 * Result of pre-filling brewing parameters from history
 */
data class BrewingParametersPrefill(
    val teaQuantityGrams: Float?,
    val waterQuantityMl: Double?,
    val temperatureCelsius: Double?,
    val brewingTime: Duration?,
    val waterType: WaterType?,
    val rinsed: Boolean = false,
    val source: PrefillSource,
) {
    companion object {
        /**
         * [brew]'s set-up, steep 1 at its plan's base so an untouched form replays it exactly. A
         * brew with no tea quantity was a bag.
         */
        fun of(brew: SessionWithSteeps): BrewingParametersPrefill {
            val first = brew.steeps.first()
            return BrewingParametersPrefill(
                teaQuantityGrams = brew.session.teaQuantityGrams,
                waterQuantityMl = first.waterQuantityMl,
                temperatureCelsius = first.temperatureCelsius,
                brewingTime = BrewingPlan.learnedFrom(brew.steeps).base,
                waterType = brew.session.waterType,
                rinsed = brew.session.rinsed,
                source = PrefillSource.PastBrew(brew),
            )
        }
    }
}
