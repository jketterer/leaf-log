package dev.jketterer.leaflog.presentation.ui.screens.flavors

import dev.jketterer.leaflog.domain.models.FlavorUsage
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea

data class FlavorDetailState(
    /** Kept once loaded, so a tag being merged away doesn't blank the page before it's replaced. */
    val tag: TastingTag? = null,
    val vocabulary: List<TastingTag> = emptyList(),
    val usage: FlavorUsage? = null,
    val teas: Map<String, Tea> = emptyMap(),
    val isLoading: Boolean = true,
    val dialog: FlavorDialog? = null,
    val isSaving: Boolean = false,
)

sealed interface FlavorDialog {
    data object Rename : FlavorDialog
    data object MergePicker : FlavorDialog
    data class ConfirmMerge(val target: TastingTag) : FlavorDialog
    data object ConfirmDelete : FlavorDialog
}
