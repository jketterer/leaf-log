package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.SessionWithSteeps

/**
 * Whether a finished session is worth offering as a method: at any steep count, unless it already
 * follows a method, was rated under 3 stars, or matches a saved method. Unrated sessions are offered.
 */
class ShouldOfferSaveAsMethodUseCase(
    private val checkDuplicateConfigurationUseCase: CheckDuplicateConfigurationUseCase,
) {
    suspend operator fun invoke(session: SessionWithSteeps): Boolean {
        val rating = session.effectiveRating
        return session.session.usedConfigurationId == null &&
            (rating == null || rating >= 3f) &&
            !checkDuplicateConfigurationUseCase(session)
    }
}
