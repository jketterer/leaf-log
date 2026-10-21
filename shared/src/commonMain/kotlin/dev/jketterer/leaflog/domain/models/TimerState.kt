package dev.jketterer.leaflog.domain.models

import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

data class TimerState(
    val sessionId: String? = null,
    val teaId: String? = null,
    val teaName: String = "",
    val steepNumber: Int = 1,
    val totalDuration: Duration = Duration.ZERO,
    val remainingDuration: Duration = Duration.ZERO,
    val status: TimerStatus = TimerStatus.NOT_STARTED,
    val startedAt: Instant? = null,
    val pausedAt: Instant? = null,
    // Drives how long after the steep to remind the user to finish the session
    val brewingTemperatureCelsius: Double? = null,
    // The plan's next steep, shown on the lock screen. Null when there is no next to offer
    val nextSteepDuration: Duration? = null,
    // A timer with no session keeps the steeps it has finished, so attaching a tea records them
    val finishedSteeps: List<TimedSteep> = emptyList(),
) {
    /** Time brewed so far: adjustments count, pauses don't. */
    val elapsed: Duration
        get() = totalDuration - remainingDuration

    /**
     * Progress from 0.0 (start) to 1.0 (complete).
     */
    val progress: Float
        get() = if (totalDuration > Duration.ZERO) {
            (elapsed.inWholeMilliseconds / totalDuration.inWholeMilliseconds.toFloat()).coerceIn(
                0f,
                1f
            )
        } else {
            0f
        }

    val isRunning: Boolean
        get() = status == TimerStatus.RUNNING

    val canPause: Boolean
        get() = status == TimerStatus.RUNNING

    val canResume: Boolean
        get() = status == TimerStatus.PAUSED

    val isComplete: Boolean
        get() = status == TimerStatus.COMPLETE

    /**
     * Re-derives the timer against wall-clock time, bringing a [remainingDuration]
     * captured earlier up to date. A RUNNING state whose duration has already elapsed
     * resolves to COMPLETE.
     */
    fun resolvedAt(now: Instant): TimerState {
        if (status != TimerStatus.RUNNING || startedAt == null) return this

        val remaining = (totalDuration - (now - startedAt)).coerceAtLeast(Duration.ZERO)
        return if (remaining == Duration.ZERO) {
            copy(status = TimerStatus.COMPLETE, remainingDuration = Duration.ZERO)
        } else {
            copy(remainingDuration = remaining)
        }
    }

    /** The steep a completed timer brewed, in whole seconds so a late tap isn't replayed. */
    val brewed: TimedSteep?
        get() = startedAt?.takeIf { isComplete }?.let { start ->
            val seconds = totalDuration.toDouble(DurationUnit.SECONDS).roundToInt().coerceAtLeast(1)
            TimedSteep(start, seconds.seconds)
        }

    /**
     * Gongfu by feel, for a completed timer with no session: the next steep brews 5s longer than
     * this one did, and waits for Start.
     */
    fun nextByFeel(): TimerState {
        val brewed = brewed ?: return this
        val length = brewed.length + 5.seconds
        return TimerState(
            steepNumber = steepNumber + 1,
            totalDuration = length,
            remainingDuration = length,
            finishedSteeps = finishedSteeps + brewed,
        )
    }

    /** Completes the timer at [now]. One stopped early ran for what had [elapsed], not its total. */
    fun completedAt(now: Instant): TimerState {
        val resolved = resolvedAt(now)
        return resolved.copy(
            status = TimerStatus.COMPLETE,
            totalDuration = resolved.elapsed,
            remainingDuration = Duration.ZERO,
        )
    }
}

/** A steep a timer with no session finished: when it started and how long it brewed. */
data class TimedSteep(val startedAt: Instant, val length: Duration)

/**
 * Timer status enum.
 */
enum class TimerStatus {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    COMPLETE,
}