package dev.jketterer.leaflog.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import org.koin.core.context.GlobalContext

/**
 * Backup receiver for timer completion. Fires via AlarmManager if the foreground
 * service is killed by aggressive battery optimization before the coroutine completes.
 */
class TimerCompletionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val teaName = intent.getStringExtra(EXTRA_TEA_NAME)?.ifEmpty { "Your tea" } ?: "Your tea"
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)

        val notificationService = GlobalContext.get().get<TimerNotificationService>()
        notificationService.showTimerComplete(teaName, sessionId)
    }

    companion object {
        const val EXTRA_TEA_NAME = "tea_name"
        const val EXTRA_SESSION_ID = "session_id"
    }
}
