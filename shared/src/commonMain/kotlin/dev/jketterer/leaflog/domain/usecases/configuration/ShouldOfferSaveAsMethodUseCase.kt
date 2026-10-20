package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository

/**
 * Whether a finished session is worth offering as a method: at any steep count, unless it was rated
 * under 3 stars or matches a saved method. A session that follows a method is offered it only on a
 * different vessel, which is a different method; any other difference updates that one instead.
 * Unrated sessions are offered.
 */
class ShouldOfferSaveAsMethodUseCase(
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val checkDuplicateConfigurationUseCase: CheckDuplicateConfigurationUseCase,
) {
    suspend operator fun invoke(session: SessionWithSteeps): Boolean {
        val rating = session.effectiveRating
        val used = session.session.usedConfigurationId?.let { brewingConfigurationRepository.getById(it) }
        return (used == null || used.vesselId != session.session.vesselId) &&
            (rating == null || rating >= 3f) &&
            !checkDuplicateConfigurationUseCase(session)
    }
}
