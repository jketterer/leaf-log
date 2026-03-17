package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Use case to start a brewing timer.
 * Creates an in-progress session if one doesn't exist.
 */
class StartTimerUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(
        session: TeaSession,
        duration: Duration,
    ): Result<TimerState> {
        if (duration <= Duration.ZERO) {
            return Result.failure(IllegalArgumentException("Duration must be greater than 0"))
        }

        return try {
            if (session.status == SessionStatus.COMPLETED) {
                return Result.failure(IllegalStateException("Cannot start timer for completed session"))
            }

            teaSessionRepository.upsert(session.copy(status = SessionStatus.IN_PROGRESS))

            val now = Clock.System.now()
            val timerState = TimerState(
                sessionId = session.id,
                teaId = session.teaId,
                teaName = "",  // Will be populated by ViewModel from tea data
                steepNumber = session.steepNumber,
                totalDuration = duration,
                remainingDuration = duration,
                status = TimerStatus.RUNNING,
                startedAt = now,
                pausedAt = null,
            )

            Result.success(timerState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}