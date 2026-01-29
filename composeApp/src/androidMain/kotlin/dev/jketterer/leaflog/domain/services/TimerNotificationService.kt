package dev.jketterer.leaflog.domain.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.jketterer.leaflog.R
import dev.jketterer.leaflog.MainActivity
import dev.jketterer.leaflog.domain.models.TimerState

/**
 * Manages timer notifications for Android.
 */
actual class TimerNotificationService(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Brewing Timer",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for brewing timer countdown and completion"
//                setSound(null, null)  // TODO: Custom sound in completion notification
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show ongoing timer notification while brewing.
     */
    actual fun showTimerRunning(state: TimerState) {
        val minutes = state.remainingDuration.inWholeMinutes
        val seconds = state.remainingDuration.inWholeSeconds
        val remainingTime = "%d:%02d".format(minutes, seconds)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "timer")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🍵 ${state.teaName}")
            .setContentText("$remainingTime remaining")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show completion notification when timer finishes.
     */
    actual fun showTimerComplete(teaName: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "timer")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("✅ $teaName is ready!")
            .setContentText("Time to enjoy your tea ☕")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancel/remove timer notification.
     */
    actual fun cancelNotifications() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1001
    }
}