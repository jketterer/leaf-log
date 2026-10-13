package dev.jketterer.leaflog.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TimerStateTest {

    @Test
    fun `a timer that ran out keeps its total including adjustments`() {
        val adjusted = TimerState(
            totalDuration = 35.seconds,
            remainingDuration = 0.seconds,
            status = TimerStatus.COMPLETE,
        )

        assertEquals(35.seconds, adjusted.completedAt(START + 60.seconds).elapsed)
    }

    @Test
    fun `completing a running timer early counts only what has elapsed`() {
        val running = TimerState(
            totalDuration = 30.seconds,
            remainingDuration = 30.seconds,
            status = TimerStatus.RUNNING,
            startedAt = START,
        )

        val completed = running.completedAt(START + 12.seconds)

        assertEquals(TimerStatus.COMPLETE, completed.status)
        assertEquals(12.seconds, completed.totalDuration)
        assertEquals(Duration.ZERO, completed.remainingDuration)
    }

    @Test
    fun `completing a paused timer leaves the pause out`() {
        val paused = TimerState(
            totalDuration = 30.seconds,
            remainingDuration = 18.seconds,
            status = TimerStatus.PAUSED,
            startedAt = START,
            pausedAt = START + 12.seconds,
        )

        assertEquals(12.seconds, paused.completedAt(START + 300.seconds).elapsed)
    }

    private companion object {
        val START = Instant.fromEpochMilliseconds(1_000_000)
    }
}
