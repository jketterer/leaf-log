package dev.jketterer.leaflog.presentation.ui.screens.sessionreview

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.usecases.session.VsBest

data class SessionReviewState(
    val session: SessionWithSteeps? = null,
    val tea: Tea? = null,
    val teaTypeColorHex: String? = null,
    val vessel: BrewingVessel? = null,
    /** The best session's vessel, for a vessel difference to name. */
    val bestVessel: BrewingVessel? = null,
    val vsBest: VsBest? = null,
    val vocabulary: List<TastingTag> = emptyList(),
    // The session's note, apart from each steep's
    val notes: String = "",
    // The steep row being edited
    val editingSteepId: String? = null,
    val editRating: Float = 0f,
    val editNotes: String = "",
    val isLoading: Boolean = true,
)
