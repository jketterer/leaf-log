package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor

/**
 * Deletes the session and all its steeps. Photo files are kept. Stopping the timer and withdrawing
 * the reminder are the caller's, after this succeeds.
 */
class DeleteSessionUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val recomputeStats: RecomputeStatsUseCase,
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> = runCatching {
        transactor {
            val current = teaSessionRepository.getSession(sessionId)
                ?: error("Session not found: $sessionId")
            teaSessionRepository.deleteSession(sessionId)
            recomputeStats(current.session.teaId)
        }
    }
}
