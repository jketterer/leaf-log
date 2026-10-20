package dev.jketterer.leaflog.presentation.ui.screens.sessionreview

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.usecases.session.VsBest

data class SessionReviewState(
    val session: SessionWithSteeps? = null,
    val tea: Tea? = null,
    val vessel: BrewingVessel? = null,
    /** The best session's vessel, for a vessel difference to name. */
    val bestVessel: BrewingVessel? = null,
    val vsBest: VsBest? = null,
    val vocabulary: List<TastingTag> = emptyList(),
    // Steep 1's note, which is the session's
    val notes: String = "",
    // Save as method: offered until saved
    val offerMethod: Boolean = false,
    val methodLabel: String = "",
    val methodSaved: Boolean = false,
    // Update method: the method this brew deviated from, offered until updated
    val methodToUpdate: BrewingConfiguration? = null,
    val methodUpdated: Boolean = false,
    // The steep row being edited
    val editingSteepId: String? = null,
    val editRating: Float = 0f,
    val editNotes: String = "",
    val isLoading: Boolean = true,
)
