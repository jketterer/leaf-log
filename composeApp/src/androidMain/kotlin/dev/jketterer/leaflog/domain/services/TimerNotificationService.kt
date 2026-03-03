package dev.jketterer.leaflog.domain.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import dev.jketterer.leaflog.MainActivity
import dev.jketterer.leaflog.R
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus

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
     * Build the expanded (big content) notification view with Chronometer countdown,
     * tea name, steep label, and a progress bar showing remaining brew time.
     *
     * The Chronometer widget auto-updates on-screen without requiring per-second
     * notification rebuilds — analogous to iOS's `Text(endDate, style: .timer)`.
     */
    private fun buildExpandedView(state: TimerState): RemoteViews {
        val isRunning = state.status == TimerStatus.RUNNING
        val remainingMillis = state.remainingDuration.inWholeMilliseconds
        val totalMillis = state.totalDuration.inWholeMilliseconds
        val steepLabel = "Steep ${state.steepNumber}"

        val views = RemoteViews(context.packageName, R.layout.notification_timer_running)

        views.setTextViewText(R.id.tv_tea_name, state.teaName)
        views.setTextViewText(
            R.id.tv_steep_label,
            if (isRunning) steepLabel else "Paused · $steepLabel",
        )

        if (isRunning) {
            // Chronometer counts down automatically — no per-second update needed
            views.setViewVisibility(R.id.chronometer_countdown, View.VISIBLE)
            views.setViewVisibility(R.id.tv_paused_time, View.GONE)
            // Set direction before starting — avoids a one-frame flicker where the
            // Chronometer briefly renders in count-up mode before the flag arrives.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                views.setChronometerCountDown(R.id.chronometer_countdown, true)
            }
            val chronoBase = SystemClock.elapsedRealtime() + remainingMillis
            views.setChronometer(R.id.chronometer_countdown, chronoBase, null, true)
        } else {
            // Static remaining time when paused — Chronometer would drift if left running
            views.setViewVisibility(R.id.chronometer_countdown, View.GONE)
            views.setViewVisibility(R.id.tv_paused_time, View.VISIBLE)
            val minutes = state.remainingDuration.inWholeMinutes
            val seconds = state.remainingDuration.inWholeSeconds % 60
            views.setTextViewText(R.id.tv_paused_time, "%d:%02d".format(minutes, seconds))
        }

        // Progress bar drains from full → empty as brew time elapses
        val progress = if (totalMillis > 0) {
            ((remainingMillis.toFloat() / totalMillis.toFloat()) * 1000).toInt().coerceIn(0, 1000)
        } else {
            0
        }
        views.setProgressBar(R.id.progress_brew, 1000, progress, false)

        return views
    }

    /**
     * Build a notification for the foreground service (does not post it).
     *
     * Compact view: tea name + steep number in header, auto-updating countdown
     * via [NotificationCompat.Builder.setUsesChronometer] (running) or static
     * remaining time text (paused), plus a draining progress bar.
     *
     * Expanded view: custom layout with large Chronometer countdown and progress bar.
     */
    internal fun buildRunningNotification(state: TimerState): Notification {
        val isRunning = state.status == TimerStatus.RUNNING
        val remainingMillis = state.remainingDuration.inWholeMilliseconds
        val totalMillis = state.totalDuration.inWholeMilliseconds

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_RUNNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(state.teaName)
            .setSubText("Steep ${state.steepNumber}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createTimerPendingIntent(state.sessionId))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setCustomBigContentView(buildExpandedView(state))

        if (isRunning) {
            // Compact countdown auto-updates in the notification header timestamp area
            builder
                .setWhen(System.currentTimeMillis() + remainingMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
        } else {
            val minutes = state.remainingDuration.inWholeMinutes
            val seconds = state.remainingDuration.inWholeSeconds % 60
            builder
                .setContentText("Paused · %d:%02d".format(minutes, seconds))
                .setShowWhen(false)
        }

        // Draining progress bar visible in the compact notification
        if (totalMillis > 0) {
            val progress = ((remainingMillis.toFloat() / totalMillis.toFloat()) * 1000)
                .toInt().coerceIn(0, 1000)
            builder.setProgress(1000, progress, false)
        }

        return builder.build()
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
