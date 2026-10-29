package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.MIN_STEEP
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.roundedForPlan
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

/**
 * The plan [session] follows: the session Brew Again copied, or, with none or one since deleted,
 * its own steeps so far (gongfu by feel: 20s, 25s carries on to 30s).
 *
 * A source's plan starts from this session's own steep 1, and its overrides move by the same
 * difference, so a longer first steep keeps the source's curve, started later.
 */
class ResolvePlanUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(session: SessionWithSteeps): BrewingPlan {
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
