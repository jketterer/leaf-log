package dev.jketterer.leaflog.presentation.ui.screens.timer

import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

/**
 * All possible user actions on the Timer screen.
 */
sealed interface TimerIntent {
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
    data object RestartTimer : TimerIntent
    /** Already brewed before Start, Finish early after. */
    data object CompleteNow : TimerIntent
    data object DismissNotificationWarning : TimerIntent
    data object BackClicked : TimerIntent

    // Quick Timer: ending it records nothing; a tea attached makes a session of its steeps
    data object JustATimer : TimerIntent
    data object ConfirmEndTimer : TimerIntent
    data object CancelEndTimer : TimerIntent
    data object NextByFeel : TimerIntent
    data object ShowTeaPicker : TimerIntent
    data object HideTeaPicker : TimerIntent
    data class TeaPicked(val teaId: String) : TimerIntent
    data class QuickAddTea(val name: String, val teaTypeId: String) : TimerIntent

    // Edit session parameters
    data object EditSession : TimerIntent
    data class EditTemperatureChanged(val celsius: Double?) : TimerIntent
    data class EditWaterQuantityChanged(val milliliters: Double?) : TimerIntent
    data class EditTeaQuantityChanged(val grams: Double?) : TimerIntent
    data class EditTeaBagModeChanged(val isTeaBag: Boolean) : TimerIntent
    data class EditWaterTypeChanged(val waterType: WaterType) : TimerIntent
    data object ConfirmEditSession : TimerIntent
    data object CancelEditSession : TimerIntent
}
