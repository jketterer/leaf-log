package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.FlavorProfile
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.SteepCurve
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.viewmodel.BrewAgainSheetState
import dev.jketterer.leaflog.presentation.ui.viewmodel.InProgressDialogState

data class TeaDetailState(
    val tea: Tea? = null,
    val teaType: TeaType? = null,
    val recentSessions: List<SessionWithSteeps> = emptyList(),
    /** Every session of the tea, for opening one from the list */
    val sessionsById: Map<String, SessionWithSteeps> = emptyMap(),
    val bestBrew: SessionWithSteeps? = null,
    val flavorProfile: FlavorProfile? = null,
    val steepCurve: SteepCurve? = null,
    val tagVocabulary: List<TastingTag> = emptyList(),
    val isLoading: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val sessionPendingDelete: String? = null,
    val brewAgainSheet: BrewAgainSheetState? = null,
    val inProgressDialogState: InProgressDialogState? = null,
)