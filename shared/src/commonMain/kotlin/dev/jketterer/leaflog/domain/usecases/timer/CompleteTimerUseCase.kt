package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.time.Duration

/**
 * Use case to complete a timer.
 * Updates session timestamp to completion time.
 */
class CompleteTimerUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(
        currentState: TimerState,
        session: TeaSession,
    ): Result<TimerState> {
        return try {
            // Update session with completion (still in progress - user needs to rate/finish)
            val updatedSession = session.copy(
                status = SessionStatus.IN_PROGRESS,
            )
            teaSessionRepository.upsert(updatedSession)

            val completedState = currentState.copy(
                status = TimerStatus.COMPLETE,
                remainingDuration = Duration.ZERO,
            )

            Result.success(completedState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}