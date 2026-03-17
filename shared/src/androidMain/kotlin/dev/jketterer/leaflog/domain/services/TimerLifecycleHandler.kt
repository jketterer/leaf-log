package dev.jketterer.leaflog.domain.services

import android.content.Context
import android.content.Intent
import android.os.Build

actual class TimerLifecycleHandler(
    private val context: Context,
) {
    actual fun onTimerStarted() {
        val intent = Intent().setClassName(
            context.packageName,
            "dev.jketterer.leaflog.services.TimerForegroundService",
        ).apply {
            action = "ACTION_START"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    actual fun onTimerStopped() {
        val intent = Intent().setClassName(
            context.packageName,
            "dev.jketterer.leaflog.services.TimerForegroundService",
        ).apply {
            action = "ACTION_STOP"
        }
        context.startService(intent)
    }
}
