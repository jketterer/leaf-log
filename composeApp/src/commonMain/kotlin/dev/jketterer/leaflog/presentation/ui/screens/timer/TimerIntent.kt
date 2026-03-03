package dev.jketterer.leaflog.presentation.ui.screens.timer

import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

/**
 * All possible user actions on the Timer screen.
 */
sealed interface TimerIntent {
    data class Initialize(val sessionId: String) : TimerIntent
    data object StartTimer : TimerIntent
    data object PauseTimer : TimerIntent
    data object ResumeTimer : TimerIntent
    data class AdjustTime(val adjustment: Duration) : TimerIntent
    data object ResetTimer : TimerIntent
    data object ConfirmReset : TimerIntent
    data object CancelReset : TimerIntent
    data object DiscardSession : TimerIntent
    data object ConfirmStop : TimerIntent
    data object CancelStop : TimerIntent
    data object ToggleTemperatureUnit : TimerIntent
    data object ToggleVolumeUnit : TimerIntent
    data object RestartTimer : TimerIntent
    data object BackClicked : TimerIntent

    // Edit session parameters
    data object EditSession : TimerIntent
    data class EditTemperatureChanged(val value: String) : TimerIntent
    data class EditWaterQuantityChanged(val value: String) : TimerIntent
    data class EditTeaQuantityChanged(val value: String) : TimerIntent
    data class EditTeaBagModeChanged(val isTeaBag: Boolean) : TimerIntent
    data class EditWaterTypeChanged(val waterType: WaterType) : TimerIntent
    data object ConfirmEditSession : TimerIntent
    data object CancelEditSession : TimerIntent
}