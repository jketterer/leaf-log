package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock

/**
 * Marks the session completed; a session that already is comes back unchanged. Stopping the timer
 * and withdrawing the reminder are the caller's, after this succeeds.
 */
class FinishSessionUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val recomputeStats: RecomputeStatsUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(sessionId: String): Result<SessionWithSteeps> = runCatching {
        transactor {
            val current = teaSessionRepository.getSession(sessionId)
                ?: throw DomainFailure.NotFound("session $sessionId")
            if (current.session.status == SessionStatus.COMPLETED) return@transactor current

            val updated = current.withSession(
                current.session.copy(status = SessionStatus.COMPLETED),
                clock.now(),
            )
            teaSessionRepository.saveSession(updated)
            recomputeStats(current.session.teaId)
            updated
        }
    }
}
