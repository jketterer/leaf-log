package dev.jketterer.leaflog.domain.services

/**
 * Platform bridge for timer lifecycle management.
 *
 * - Android: starts/stops a foreground service to keep the process alive
 * - iOS: no-op (iOS handles background execution differently)
 */
expect class TimerLifecycleHandler {
    fun onTimerStarted()
    fun onTimerStopped()
}
