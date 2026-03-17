package dev.jketterer.leaflog.domain.usecases.session

/**
 * Describes the source of pre-filled brewing parameters
 */
sealed interface PrefillSource {
    /**
     * Pre-filled from a saved brewing configuration for this exact tea + vessel
     */
    data object SavedConfig : PrefillSource

    /**
     * Pre-filled from a saved brewing configuration for the same tea type + vessel
     */
    data class SameTypeConfig(val teaName: String) : PrefillSource

    /**
     * No pre-fill available
     */
    data object None : PrefillSource
}
