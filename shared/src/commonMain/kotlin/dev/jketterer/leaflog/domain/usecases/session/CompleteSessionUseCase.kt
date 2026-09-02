package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.services.TimerService
import kotlin.time.Clock

class CompleteSessionUseCase(
    private val teaSessionRepository: TeaSessionRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val updateTeaStatsUseCase: UpdateTeaStatsUseCase,
    private val timerService: TimerService,
) {
    suspend operator fun invoke(
        session: TeaSession,
        rating: Float? = null,
        finalNotes: String? = null
    ): Result<TeaSession> {
        if (rating != null && (rating !in 0f..5f)) {
            return Result.failure(IllegalArgumentException("Rating must be between 0 and 5"))
        }

        // Update session to completed
        val completedSession = session.copy(
            status = SessionStatus.COMPLETED,
            rating = rating,
            notes = finalNotes?.takeIf { it.isNotBlank() }?.trim() ?: session.notes,
            updatedAt = Clock.System.now()
        )

        return try {
            if (timerService.getCurrentState().sessionId == session.id) {
                timerService.stop()
            }
            // Withdraw the finish-your-session reminder. Kept separate from the stop above: by the
            // time a session is completed the timer has usually already been stopped and its state
            // cleared, so the id check above no longer matches.
            timerService.onSessionResolved(session.id)

            teaSessionRepository.upsert(completedSession)
            updateTeaStatsUseCase(session.teaId)
            if (session.usedConfigurationId != null) {
                brewingConfigurationRepository.incrementTimesUsed(
                    session.usedConfigurationId,
                    Clock.System.now(),
                )
            }
            Result.success(completedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
