package dev.jketterer.leaflog.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import org.koin.core.context.GlobalContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Backup receiver for timer completion. Fires via AlarmManager if the foreground
 * service is killed by aggressive battery optimization before the coroutine completes.
 */
class TimerCompletionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = TimerState(
            sessionId = intent.getStringExtra(EXTRA_SESSION_ID),
            teaName = intent.getStringExtra(EXTRA_TEA_NAME).orEmpty(),
            steepNumber = intent.getIntExtra(EXTRA_STEEP_NUMBER, 1),
            nextSteepDuration = intent.getLongExtra(EXTRA_NEXT_STEEP_MS, -1L)
                .takeIf { it >= 0 }?.milliseconds,
        )

        val notificationService = GlobalContext.get().get<TimerNotificationService>()
        notificationService.showTimerComplete(state)
    }

    companion object {
        const val EXTRA_TEA_NAME = "tea_name"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_STEEP_NUMBER = "steep_number"
        const val EXTRA_NEXT_STEEP_MS = "next_steep_ms"
    }
}
