package dev.jketterer.leaflog.domain.services

actual class TimerLifecycleHandler {
    actual fun onTimerStarted() {
        // No-op on iOS — scheduled notifications handle background completion
    }

    actual fun onTimerStopped() {
        // No-op on iOS
    }
}
