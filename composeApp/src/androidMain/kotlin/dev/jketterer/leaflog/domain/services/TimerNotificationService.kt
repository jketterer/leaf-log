package dev.jketterer.leaflog.domain.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.jketterer.leaflog.MainActivity
import dev.jketterer.leaflog.R
import dev.jketterer.leaflog.domain.models.TimerState

/**
 * Manages timer notifications for Android.
 */
class TimerNotificationServiceImpl(private val context: Context) : TimerNotificationService {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val runningChannel = NotificationChannel(
                CHANNEL_ID_RUNNING,
                "Brewing Timer",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Ongoing notification while your tea is brewing"
            }
            val completeChannel = NotificationChannel(
                CHANNEL_ID_COMPLETE,
                "Brewing Timer Complete",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alert when your tea is done brewing"
            }
            notificationManager.createNotificationChannel(runningChannel)
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    private fun createTimerPendingIntent(sessionId: String? = null): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "timer")
            sessionId?.let { putExtra("sessionId", it) }
        }
        return PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Build a notification for the foreground service (does not post it).
     */
    fun buildRunningNotification(state: TimerState): Notification {
        val minutes = state.remainingDuration.inWholeMinutes
        val seconds = state.remainingDuration.inWholeSeconds % 60
        val remainingTime = "%d:%02d".format(minutes, seconds)

        return NotificationCompat.Builder(context, CHANNEL_ID_RUNNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(state.teaName)
            .setContentText("$remainingTime remaining")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createTimerPendingIntent(state.sessionId))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    /**
     * Show ongoing timer notification while brewing.
     */
    override fun showTimerRunning(state: TimerState) {
        notificationManager.notify(NOTIFICATION_ID, buildRunningNotification(state))
    }

    /**
     * Show completion notification when timer finishes.
     */
    override fun showTimerComplete(teaName: String, sessionId: String?) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$teaName is ready!")
            .setContentText("Time to enjoy your tea")
            .setAutoCancel(true)
            .setContentIntent(createTimerPendingIntent(sessionId))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        // Use a separate ID so the foreground service lifecycle (stopForeground) doesn't
        // remove this notification when the running notification (NOTIFICATION_ID) is cleared.
        // Using the same ID as the running notification caused it to be wiped by stopForeground,
        // or suppressed as a "re-notification" of a user-dismissed notification.
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    /**
     * No-op on Android — the foreground service keeps the process alive,
     * so the coroutine-based countdown fires the completion notification directly.
     */
    override fun scheduleCompletionAlarm(
        teaName: String,
        remainingSeconds: Double,
        sessionId: String?
    ) {
        // Intentionally empty
    }

    /**
     * No-op on Android.
     */
    override fun cancelCompletionAlarm() {
        // Intentionally empty
    }

    /**
     * No-op on Android — no Live Activity equivalent.
     */
    override fun onTimerStopped() {
        // Intentionally empty
    }

    companion object {
        private const val CHANNEL_ID_RUNNING = "timer_running"
        private const val CHANNEL_ID_COMPLETE = "timer_complete"
        const val NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002
    }
}
