package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState

interface TimerNotificationService {
    fun showTimerRunning(state: TimerState)
    fun showTimerComplete(teaName: String, sessionId: String?)
    fun scheduleCompletionAlarm(teaName: String, remainingSeconds: Double, sessionId: String?)
    fun cancelCompletionAlarm()
    fun onTimerStopped()
}