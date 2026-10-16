package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock

/**
 * Deletes steep 2 or later and renumbers the rest so they stay 1…n. Steep 1 is the session, so it
 * goes with [DeleteSessionUseCase]. Photo files are kept.
 */
class DeleteSteepUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val recomputeStats: RecomputeStatsUseCase,
) {
    suspend operator fun invoke(steepId: String): Result<SessionWithSteeps> = runCatching {
        transactor {
            val current = teaSessionRepository.getSessionOfSteep(steepId)
                ?: throw DomainFailure.NotFound("steep $steepId")
            require(steepId != current.session.id) { "Steep 1 is the session: delete the session" }

            teaSessionRepository.deleteSteep(steepId)
            val now = Clock.System.now()
            val updated = current.copy(
                steeps = current.steeps
                    .filter { it.id != steepId }
                    .mapIndexed { index, steep ->
                        if (steep.number == index + 1) steep
                        else steep.copy(number = index + 1, updatedAt = now)
                    },
            )
            teaSessionRepository.saveSession(updated)
            recomputeStats(current.session.teaId)
            updated
        }
    }
}
