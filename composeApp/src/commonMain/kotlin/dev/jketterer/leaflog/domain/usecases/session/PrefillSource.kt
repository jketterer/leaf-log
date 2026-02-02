package dev.jketterer.leaflog.domain.usecases.session

import kotlin.time.Instant

/**
 * Describes the source of pre-filled brewing parameters
 */
sealed interface PrefillSource {
    /**
     * Pre-filled from a previous session with this exact tea + vessel combination
     */
    data class DirectSession(
        val sessionId: String,
        val rating: Float,
        val timestamp: Instant,
    ) : PrefillSource

    /**
     * Pre-filled from a session with the same tea type + vessel
     */
    data class TeaTypeFallback(
        val sessionId: String,
        val teaName: String,
        val rating: Float,
    ) : PrefillSource

    /**
     * Pre-filled from tea defaults
     */
    data object TeaDefaults : PrefillSource

    /**
     * No pre-fill available
     */
    data object None : PrefillSource
}
