package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock

/**
 * Applies [edit] to the steep as stored, so callers name only what they change and `null` clears.
 * Only the brew parameters, rating, notes, photos and tags can change.
 */
class EditSteepUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val recomputeStats: RecomputeStatsUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        steepId: String,
        edit: (Steep) -> Steep,
    ): Result<SessionWithSteeps> = runCatching {
        transactor {
            val current = teaSessionRepository.getSessionOfSteep(steepId)
                ?: throw DomainFailure.NotFound("steep $steepId")
            val steep = current.steeps.first { it.id == steepId }
            val edited = edit(steep).let { it.copy(notes = it.notes.blankToNull()) }

            val untouched = edited.copy(
                brewingTime = steep.brewingTime,
                actualTime = steep.actualTime,
                temperatureCelsius = steep.temperatureCelsius,
                waterQuantityMl = steep.waterQuantityMl,
                rating = steep.rating,
                notes = steep.notes,
                photos = steep.photos,
                tags = steep.tags,
            )
            require(untouched == steep) {
                "Only a steep's brew parameters, rating, notes, photos and tags can be edited"
            }
            edited.requireValid()

            val now = clock.now()
            val updated = current.copy(
                steeps = current.steeps.map {
                    if (it.id == steepId) edited.copy(updatedAt = now) else it
                },
            )
            teaSessionRepository.saveSession(updated)
            recomputeStats(current.session.teaId)
            updated
        }
    }
}
