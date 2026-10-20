package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import kotlin.time.Clock
import kotlin.time.Instant

/** Makes a method follow a session's brew: its set-up and the plan its steeps followed. */
class UpdateMethodFromSessionUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
) {
    suspend operator fun invoke(methodId: String, session: SessionWithSteeps): Result<BrewingConfiguration> =
        runCatching {
            val method = requireNotNull(brewingConfigurationRepository.getById(methodId)) {
                "Configuration not found"
            }
            method.updatedFrom(session, Clock.System.now()).also { brewingConfigurationRepository.upsert(it) }
        }
}

/**
 * The method after learning from the session: the first steep's water and temperature, the session's
 * leaf and water type, and the learned plan over the steeps brewed. The method's overrides past the
 * last steep brewed stay.
 */
internal fun BrewingConfiguration.updatedFrom(session: SessionWithSteeps, now: Instant): BrewingConfiguration {
    val first = session.steeps.first()
    val learned = BrewingPlan.learnedFrom(session.steeps)
    val brewed = session.steeps.maxOf { it.number }
    return copy(
        teaQuantityGrams = session.session.teaQuantityGrams,
        waterQuantityMl = first.waterQuantityMl,
        temperatureCelsius = first.temperatureCelsius,
        waterType = session.session.waterType,
        brewingTime = learned.base,
        steepIncrement = learned.increment,
        steepOverrides = learned.overrides + steepOverrides.filterKeys { it > brewed },
        updatedAt = now,
    )
}
