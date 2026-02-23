package dev.jketterer.leaflog.domain.services

import android.content.Context
import android.content.Intent
import android.os.Build
import dev.jketterer.leaflog.services.TimerForegroundService

actual class TimerLifecycleHandler(
    private val context: Context,
) {
    actual fun onTimerStarted() {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    actual fun onTimerStopped() {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
