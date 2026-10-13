package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository

/**
 * Abandons the steep being brewed. On steep 1 nothing happened yet, so the session goes; on a
 * later steep the earlier ones did happen, so that steep goes and the session is finished.
 * Stopping the timer and withdrawing the reminder are the caller's, after this succeeds.
 */
class DiscardSteepUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val deleteSession: DeleteSessionUseCase,
    private val deleteSteep: DeleteSteepUseCase,
    private val finishSession: FinishSessionUseCase,
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> = runCatching {
        val current = teaSessionRepository.getSession(sessionId)
            ?: error("Session not found: $sessionId")
        if (current.steeps.size == 1) {
            deleteSession(sessionId).getOrThrow()
        } else {
            deleteSteep(current.currentSteep.id).getOrThrow()
            finishSession(sessionId).getOrThrow()
        }
    }
}
