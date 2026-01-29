package dev.jketterer.leaflog.domain.models

import kotlin.time.Duration
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
) {
    /**
     * Progress from 0.0 (start) to 1.0 (complete).
     */
    val progress: Float
        get() = if (totalDuration > Duration.ZERO) {
            val elapsed = totalDuration - remainingDuration
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
}

/**
 * Timer status enum.
 */
enum class TimerStatus {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    COMPLETE,
}