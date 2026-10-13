package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock

/**
 * Stores the timer on the steep it's timing, [TimerState.steepNumber] of session
 * [TimerState.sessionId], so a brew survives the app being killed. Timer fields don't feed any
 * stats, so nothing is recomputed.
 */
class SaveTimerStateUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(timerState: TimerState): Result<Unit> = runCatching {
        val sessionId = requireNotNull(timerState.sessionId) { "Cannot save a timer with no session" }
        val now = Clock.System.now()
        transactor {
            val current = teaSessionRepository.getSession(sessionId)
                ?: error("Session not found: $sessionId")
            require(current.steeps.any { it.number == timerState.steepNumber }) {
                "Session $sessionId has no steep ${timerState.steepNumber}"
            }
            val steeps = current.steeps.map {
                if (it.number != timerState.steepNumber) return@map it
                it.copy(
                    timerStatus = timerState.status,
                    timerStartedAt = timerState.startedAt,
                    timerPausedAt = if (timerState.status == TimerStatus.PAUSED) now else timerState.pausedAt,
                    timerRemainingMs = timerState.remainingDuration.inWholeMilliseconds,
                    timerTotalMs = timerState.totalDuration.inWholeMilliseconds,
                    updatedAt = now,
                )
            }
            teaSessionRepository.saveSession(current.copy(steeps = steeps))
        }
    }
}
