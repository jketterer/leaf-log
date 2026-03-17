package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock

/**
 * Updates the average rating for a parent session based on all steep ratings.
 *
 * Collects ratings from the parent session (Steep 1) and all child steeps,
 * calculates the average, and truncates to 1 decimal place.
 */
class UpdateAverageRatingUseCase(
    private val teaSessionRepository: TeaSessionRepository
) {
    suspend operator fun invoke(parentSessionId: String): Result<TeaSession> {
        return try {
            val parentSession = teaSessionRepository.getById(parentSessionId)
                ?: return Result.failure(IllegalArgumentException("Parent session not found"))

            // Get all child steeps
            val childSteeps = teaSessionRepository.getChildSteeps(parentSessionId)

            // Collect ratings from parent session and all child steeps
            val allRatings = buildList {
                parentSession.rating?.let { add(it) }
                childSteeps.forEach { steep ->
                    steep.rating?.let { add(it) }
                }
            }

            // Calculate average rating and truncate to 1 decimal place
            val updatedParent = if (allRatings.isNotEmpty()) {
                val rawAverage = allRatings.average()
                val truncatedAverage = (rawAverage * 10).toInt() / 10.0f

                parentSession.copy(
                    averageRating = truncatedAverage,
                    updatedAt = Clock.System.now()
                )
            } else {
                // No ratings available, clear average rating
                parentSession.copy(
                    averageRating = null,
                    updatedAt = Clock.System.now()
                )
            }

            teaSessionRepository.upsert(updatedParent)
            Result.success(updatedParent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
