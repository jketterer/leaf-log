package dev.jketterer.leaflog.domain.usecases.timer

import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class AdjustTimeUseCaseTest {

    private val adjustTime = AdjustTimeUseCase()

    @Test
    fun `taking more time off than a timer not yet started has leaves one second`() {
        val notStarted = TimerState(totalDuration = 20.seconds, remainingDuration = 20.seconds)

        val adjusted = adjustTime(notStarted, (-1).minutes).getOrThrow()

        assertEquals(1.seconds, adjusted.totalDuration)
        assertEquals(1.seconds, adjusted.remainingDuration)
    }

    @Test
    fun `adjusting a paused timer leaves the time spent paused out`() {
        // Paused 12s into a 30s steep, then left paused for a long while
        val paused = TimerState(
            totalDuration = 30.seconds,
            remainingDuration = 18.seconds,
            status = TimerStatus.PAUSED,
            startedAt = Instant.fromEpochMilliseconds(0),
            pausedAt = Instant.fromEpochMilliseconds(12_000),
        )

        val adjusted = adjustTime(paused, 10.seconds).getOrThrow()

        assertEquals(28.seconds, adjusted.remainingDuration)
        assertEquals(40.seconds, adjusted.totalDuration)
    }
}
