package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.roundedForPlan
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlin.math.abs

/**
 * The method a finished session should offer to update: the one it used, when it was brewed on the
 * same vessel, wasn't rated under 3 stars and deviated from the method. Null when there is none.
 */
class FindMethodUpdateUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
) {
    suspend operator fun invoke(session: SessionWithSteeps): BrewingConfiguration? {
        val rating = session.effectiveRating
        if (rating != null && rating < 3f) return null
        val method = session.session.usedConfigurationId
            ?.let { brewingConfigurationRepository.getById(it) } ?: return null
        return method.takeIf { it.vesselId == session.session.vesselId && it.deviatesFrom(session) }
    }
}

/**
 * Whether the session's set-up or any steep's time differs from the method's. Times are compared
 * rounded as a learned plan rounds them, so a late tap isn't a deviation; temperature has 1 °C of
 * slack, and the rest must match. Water and temperature are the first steep's, as in a method.
 */
internal fun BrewingConfiguration.deviatesFrom(session: SessionWithSteeps): Boolean {
    val first = session.steeps.first()
    return session.steeps.any { plan.timeOf(it.number).roundedForPlan() != it.brewedTime.roundedForPlan() } ||
        teaQuantityGrams != session.session.teaQuantityGrams ||
        waterQuantityMl != first.waterQuantityMl ||
        abs(temperatureCelsius - first.temperatureCelsius) > 1.0 ||
        waterType != session.session.waterType
}
