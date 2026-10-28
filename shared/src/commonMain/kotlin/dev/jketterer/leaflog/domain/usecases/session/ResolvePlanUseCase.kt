package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.MIN_STEEP
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.roundedForPlan
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

/**
 * The plan [session] follows right now, looked up fresh: editing its method changes the steeps to
 * come. In order: its method, the session Brew Again copied, then its own steeps so far (gongfu by
 * feel: 20s, 25s carries on to 30s). A method or source that is gone is skipped.
 *
 * A source's plan starts from this session's own steep 1, and its overrides move by the same
 * difference, so a longer first steep keeps the source's curve, started later.
 */
class ResolvePlanUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(session: SessionWithSteeps): BrewingPlan {
        val method = session.session.usedConfigurationId
            ?.let { brewingConfigurationRepository.getById(it) }
        if (method != null) return method.plan

        val source = session.session.planSessionId?.let { teaSessionRepository.getSession(it) }
            ?: return BrewingPlan.learnedFrom(session.steeps)
        val plan = BrewingPlan.learnedFrom(source.steeps)
        val base = session.steeps.first().brewedTime.roundedForPlan()
        val shift = base - plan.base
        return plan.copy(
            base = base,
            overrides = plan.overrides.mapValues { (it.value + shift).coerceAtLeast(MIN_STEEP) },
        )
    }
}
