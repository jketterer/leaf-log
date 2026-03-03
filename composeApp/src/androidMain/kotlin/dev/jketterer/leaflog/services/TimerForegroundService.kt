package dev.jketterer.leaflog.services

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl
import dev.jketterer.leaflog.domain.services.TimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TimerForegroundService : Service() {

    private val timerService: TimerService by inject()
    private val notificationService: TimerNotificationServiceImpl by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collectionJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForeground()
            ACTION_STOP -> stopSelf()
        }
        // START_NOT_STICKY: if the OS kills this service, do not restart it with a null intent.
        // TimerService holds the authoritative state; the next user interaction will re-start
        // the foreground service via TimerLifecycleHandler if a session is still active.
        return START_NOT_STICKY
    }

    private fun startForeground() {
        val initialState = timerService.getCurrentState()
        val notification = notificationService.buildRunningNotification(initialState)

        val notificationId = TimerNotificationServiceImpl.NOTIFICATION_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(notificationId, notification)
        }

        // Auto-stop when the timer completes or is cancelled
        collectionJob?.cancel()
        collectionJob = serviceScope.launch {
            timerService.timerState.collect { state ->
                if (state.status == TimerStatus.COMPLETE ||
                    state.status == TimerStatus.NOT_STARTED
                ) {
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        collectionJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}
