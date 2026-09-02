package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState

interface TimerNotificationService {
    fun showTimerRunning(state: TimerState)

    /**
     * Show the timer as paused, holding at the state's remaining time.
     * Callers must pass the already-paused state, not the last running one.
     */
    fun showTimerPaused(state: TimerState)

    fun showTimerComplete(teaName: String, sessionId: String?)
    fun scheduleCompletionAlarm(teaName: String, remainingSeconds: Double, sessionId: String?)
    fun cancelCompletionAlarm()

    /**
     * Schedule the nudge to come back and finish an unreviewed session, [delaySeconds] from now.
     * Scheduled when the steep starts rather than when it ends so it still fires if the process
     * is killed mid-brew. Cancel it whenever the session stops needing review.
     */
    fun scheduleSessionReminder(teaName: String, sessionId: String, delaySeconds: Double)
    fun cancelSessionReminder()
    fun onTimerStopped()

    /**
     * Check whether the app has permission to post notifications.
     * Returns false if the user has denied notification permission,
     * meaning background timer alerts will not be delivered.
     */
    suspend fun hasNotificationPermission(): Boolean
}