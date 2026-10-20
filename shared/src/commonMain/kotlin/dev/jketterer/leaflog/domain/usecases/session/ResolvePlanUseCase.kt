package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

/**
 * The plan [session] follows right now, looked up fresh: editing its method changes the steeps to
 * come. In order: its method, the session Brew Again copied, then its own steeps so far (gongfu by
 * feel: 20s, 25s carries on to 30s). A method or source that is gone is skipped.
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
        return BrewingPlan.learnedFrom((source ?: session).steeps)
    }
}
