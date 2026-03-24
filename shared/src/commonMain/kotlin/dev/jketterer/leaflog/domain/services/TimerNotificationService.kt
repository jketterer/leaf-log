package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState

interface TimerNotificationService {
    fun showTimerRunning(state: TimerState)
    fun showTimerComplete(teaName: String, sessionId: String?)
    fun scheduleCompletionAlarm(teaName: String, remainingSeconds: Double, sessionId: String?)
    fun cancelCompletionAlarm()
    fun onTimerStopped()

    /**
     * Check whether the app has permission to post notifications.
     * Returns false if the user has denied notification permission,
     * meaning background timer alerts will not be delivered.
     */
    suspend fun hasNotificationPermission(): Boolean
}