package dev.jketterer.leaflog.domain.services

import dev.jketterer.leaflog.domain.models.TimerState

expect class TimerNotificationService {
    fun showTimerRunning(state: TimerState)
    fun showTimerComplete(teaName: String)
    fun cancelNotifications()
}