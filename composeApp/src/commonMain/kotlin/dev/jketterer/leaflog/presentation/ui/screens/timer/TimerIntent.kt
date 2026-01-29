package dev.jketterer.leaflog.presentation.ui.screens.timer

import dev.jketterer.leaflog.domain.models.TeaSession
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
    data object StopTimer : TimerIntent
    data object ConfirmStop : TimerIntent
    data object CancelStop : TimerIntent
    data class RatingChanged(val rating: Float) : TimerIntent
    data class NotesChanged(val notes: String) : TimerIntent
    data object AddPhotoClicked : TimerIntent
    data class PhotoSelected(val photoUri: String) : TimerIntent
    data class PhotoRemoved(val photoUri: String) : TimerIntent
    data object ShowNextSteepDialog : TimerIntent
    data object CancelNextSteepDialog : TimerIntent
    data class UpdateNextSteepDuration(val duration: Duration?) : TimerIntent
    data class UpdateNextSteepTemperature(val temperature: Int) : TimerIntent
    data class UpdateNextSteepWaterQuantity(val quantity: Int) : TimerIntent
    data class ConfirmNextSteep(val session: TeaSession) : TimerIntent
    data class SaveAndFinish(val session: TeaSession) : TimerIntent
    data object RestartTimer : TimerIntent
    data object BackClicked : TimerIntent
}