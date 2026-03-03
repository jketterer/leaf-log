package dev.jketterer.leaflog.domain.services

interface LiveActivityService {
    fun start(teaName: String, steepNumber: Int, totalSeconds: Double, remainingSeconds: Double, sessionId: String?)
    fun update(remainingSeconds: Double, isPaused: Boolean)
    fun end()
}

object LiveActivityServiceHolder {
    var instance: LiveActivityService? = null
}
