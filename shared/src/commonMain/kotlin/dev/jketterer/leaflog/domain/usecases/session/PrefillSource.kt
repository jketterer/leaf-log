package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionWithSteeps

/**
 * Describes the source of pre-filled brewing parameters
 */
sealed interface PrefillSource {
    /** The pair's default brew, which a session started from these parameters replays. */
    data class PastBrew(val session: SessionWithSteeps) : PrefillSource

    /**
     * Pre-filled from this tea's own default temperature. Temperature only - there is no method
     * behind it, just a number the user recorded on the tea.
     */
    data object TeaDefault : PrefillSource

    /**
     * Pre-filled from the tea type's default temperature, when the tea has none of its own.
     */
    data class TypeDefault(val typeName: String) : PrefillSource

    /**
     * No pre-fill available
     */
    data object None : PrefillSource
}
