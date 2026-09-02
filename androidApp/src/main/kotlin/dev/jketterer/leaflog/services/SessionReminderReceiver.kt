package dev.jketterer.leaflog.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl
import org.koin.core.context.GlobalContext

/**
 * Posts the nudge to finish a session that was left unreviewed. Scheduled via AlarmManager when
 * the steep starts, so it still fires if the app never came back to the foreground.
 */
class SessionReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val teaName = intent.getStringExtra(EXTRA_TEA_NAME).orEmpty()
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return

        val notificationService = GlobalContext.get().get<TimerNotificationServiceImpl>()
        notificationService.showSessionReminder(teaName, sessionId)
    }

    companion object {
        const val EXTRA_TEA_NAME = "tea_name"
        const val EXTRA_SESSION_ID = "session_id"
    }
}
