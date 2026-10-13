package dev.jketterer.leaflog.domain.usecases.session.aggregate

import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock

/**
 * Stores the timer on the steep it's timing, [TimerState.sessionId], so a brew survives the app
 * being killed. Timer fields don't feed any stats, so nothing is recomputed.
 */
class SaveTimerStateUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(timerState: TimerState): Result<Unit> = runCatching {
        val steepId = requireNotNull(timerState.sessionId) { "Cannot save a timer with no steep" }
        val now = Clock.System.now()
        updateSteep(steepId) {
            it.copy(
                timerStatus = timerState.status,
                timerStartedAt = timerState.startedAt,
                timerPausedAt = if (timerState.status == TimerStatus.PAUSED) now else timerState.pausedAt,
                timerRemainingMs = timerState.remainingDuration.inWholeMilliseconds,
                timerTotalMs = timerState.totalDuration.inWholeMilliseconds,
                updatedAt = now,
            )
        }
    }

    suspend fun clear(steepId: String): Result<Unit> = runCatching {
        val now = Clock.System.now()
        updateSteep(steepId) {
            it.copy(
                timerStatus = null,
                timerStartedAt = null,
                timerPausedAt = null,
                timerRemainingMs = null,
                timerTotalMs = null,
                updatedAt = now,
            )
        }
    }

    private suspend fun updateSteep(steepId: String, change: (Steep) -> Steep) = transactor {
        val current = teaSessionRepository.getSessionOfSteep(steepId)
            ?: error("Steep not found: $steepId")
        teaSessionRepository.saveSession(
            current.copy(steeps = current.steeps.map { if (it.id == steepId) change(it) else it }),
        )
    }
}
