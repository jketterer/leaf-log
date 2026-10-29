package dev.jketterer.leaflog.presentation.ui.screens.flavors

import dev.jketterer.leaflog.domain.models.FlavorProfile
import dev.jketterer.leaflog.domain.models.TastingTag

data class FlavorsState(
    val vocabulary: List<TastingTag> = emptyList(),
    /** Every tag the journal's completed sessions confirmed, most used first; null before any. */
    val profile: FlavorProfile? = null,
    val isLoading: Boolean = true,
) {
    /** The tags no session confirmed, in vocabulary order. */
    val notTasted: List<TastingTag>
        get() {
            val tasted = profile?.tags.orEmpty().map { it.first.id }.toSet()
            return vocabulary.filter { it.id !in tasted }
        }
}
