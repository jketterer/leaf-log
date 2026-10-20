package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import dev.jketterer.leaflog.domain.usecases.session.RecomputeStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.withSession
import kotlin.time.Clock

/**
 * Saves a brewing configuration from a successful session: its set-up and the plan its steeps
 * followed, at any steep count.
 *
 * The session moves to the new method, so its recomputed `timesUsed` starts at 1 and any method the
 * session used before loses that use.
 */
class SaveBrewingConfigurationUseCase(
    private val transactor: Transactor,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val teaSessionRepository: TeaSessionRepository,
    private val recomputeStats: RecomputeStatsUseCase,
    private val generateConfigurationLabelUseCase: GenerateConfigurationLabelUseCase,
) {
    /**
     * @param customLabel Optional custom label (if null, auto-generates)
     */
    suspend operator fun invoke(
        session: SessionWithSteeps,
        customLabel: String? = null,
        skipRatingCheck: Boolean = false,
    ): Result<BrewingConfiguration> = runCatching {
        val rating = session.effectiveRating
        require(skipRatingCheck || rating == null || rating >= 3f) {
            "Session must not be rated under 3 stars"
        }

        val first = session.steeps.first()
        val plan = BrewingPlan.learnedFrom(session.steeps)
        val label = customLabel ?: generateConfigurationLabelUseCase(
            teaQuantityGrams = session.session.teaQuantityGrams,
            waterQuantityMl = first.waterQuantityMl,
            brewingTime = plan.base,
        )

        val now = Clock.System.now()
        val configuration = BrewingConfiguration(
            id = generateId(),
            teaId = session.session.teaId,
            vesselId = session.session.vesselId,
            teaQuantityGrams = session.session.teaQuantityGrams,
            waterQuantityMl = first.waterQuantityMl,
            temperatureCelsius = first.temperatureCelsius,
            brewingTime = plan.base,
            steepIncrement = plan.increment,
            steepOverrides = plan.overrides,
            waterType = session.session.waterType,
            rinse = session.session.rinsed,
            sourceSessionId = session.session.id,
            rating = rating,
            // Recomputed below, from the sessions that used it
            timesUsed = 0,
            lastUsedAt = null,
            label = label,
            createdAt = now,
            updatedAt = now,
        )

        transactor {
            brewingConfigurationRepository.upsert(configuration)
            val source = teaSessionRepository.getSession(session.session.id)
            if (source != null) {
                teaSessionRepository.saveSession(
                    source.withSession(source.session.copy(usedConfigurationId = configuration.id), now),
                )
            }
            recomputeStats(configuration.teaId)
        }
        brewingConfigurationRepository.getById(configuration.id) ?: configuration
    }

    private fun generateId(): String {
        return "config_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
    }
}
