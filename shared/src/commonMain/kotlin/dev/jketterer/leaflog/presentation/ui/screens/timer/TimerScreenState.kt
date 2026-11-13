package dev.jketterer.leaflog.presentation.ui.screens.timer

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

/**
 * UI State for the Timer screen.
 *
 * All properties have default values for easier testing/debugging.
 */
data class TimerScreenState(
    val timerState: TimerState = TimerState(),
    val session: SessionWithSteeps? = null,
    val tea: Tea? = null,
    val teaTypeColorHex: String? = null,
    val vessel: BrewingVessel? = null,
    /** A timer with no session, so nothing it times is recorded. */
    val isQuickTimer: Boolean = false,
    val showEndTimerConfirmation: Boolean = false,
    val showTeaPicker: Boolean = false,
    val pickerTeas: List<Tea> = emptyList(),
    val recentTeas: List<Tea> = emptyList(),
    val teaTypes: List<TeaType> = emptyList(),
    val showDiscardSessionConfirmation: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val showEditSheet: Boolean = false,
    val editTemperatureCelsius: Double? = null,
    val editWaterQuantityMl: Double? = null,
    val editTeaQuantityGrams: Double? = null,
    val editIsTeaBag: Boolean = false,
    val editWaterType: WaterType? = null,
    /** Without permission, a timer finishing in the background makes no sound. */
    val notificationsOff: Boolean = false,
    val isLoading: Boolean = false,
    val userPreferences: UserPreferences = UserPreferences(),
) {
    /** Whether the edit sheet holds anything the session doesn't, so closing it would lose it. */
    val hasEditChanges: Boolean
        get() {
            val session = session ?: return false
            val steep = session.currentSteep
            return editTemperatureCelsius != steep.temperatureCelsius ||
                    editWaterQuantityMl != steep.waterQuantityMl ||
                    editWaterType != session.session.waterType ||
                    editIsTeaBag != (session.session.teaQuantityGrams == null) ||
                    !editIsTeaBag && editTeaQuantityGrams?.toFloat() != session.session.teaQuantityGrams
        }

    /** Time left as M:SS, rounded up so the last second reads 0:01 until it's over. */
    val formattedTime: String
        get() = formatClock(
            when (timerState.status) {
                TimerStatus.RUNNING, TimerStatus.PAUSED -> timerState.remainingDuration
                else -> timerState.totalDuration
            },
        )

    /** The steep's whole length as M:SS, adjustments included. */
    val formattedTotal: String
        get() = formatClock(timerState.totalDuration)
}

/** A duration as M:SS, a fractional second rounding up. */
internal fun formatClock(duration: Duration): String {
    val milliseconds = duration.inWholeMilliseconds
    val totalSeconds = if (milliseconds > 0) (milliseconds + 999) / 1000 else 0L
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
