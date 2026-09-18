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
     * Pre-filled from the highest-rated past session of this tea + vessel. This is what makes
     * rating a steep worth doing: the parameters of a brew you liked come back next time.
     */
    data class BestRatedSession(val rating: Float) : PrefillSource

    /**
     * Pre-filled from a saved brewing configuration for the same tea type + vessel
     */
    data class SameTypeConfig(val teaName: String) : PrefillSource

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

/**
 * True when the source carried a whole brewing method, not just a stray parameter.
 *
 * Callers infer things from a pre-fill having happened (tea-bag mode is inferred from a method
 * that named no tea quantity) and a temperature default would otherwise look like a method with
 * the quantity left out. Exhaustive on purpose: a new source has to answer this.
 */
val PrefillSource.isCompleteMethod: Boolean
    get() = when (this) {
        is PrefillSource.SavedConfig,
        is PrefillSource.BestRatedSession,
        is PrefillSource.SameTypeConfig,
            -> true

        is PrefillSource.TeaDefault,
        is PrefillSource.TypeDefault,
        is PrefillSource.None,
            -> false
    }
