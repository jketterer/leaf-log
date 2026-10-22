package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.presentation.ui.viewmodel.InProgressDialogState
import kotlin.time.Duration

data class SessionDetailState(
    val session: SessionWithSteeps? = null,
    val tea: Tea? = null,
    val vessel: BrewingVessel? = null,
    val usedConfigurationLabel: String? = null,
    val vocabulary: List<TastingTag> = emptyList(),

    // Its chronological neighbours across every session, for stepping through
    val previousSessionId: String? = null,
    val nextSessionId: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showDeleteSteepConfirmation: Boolean = false,
    val steepToDelete: String? = null,
    val showAddSteepDialog: Boolean = false,
    val nextSteepDuration: Duration? = null,
    val nextSteepTemperature: Double? = null,

    // The steep open in the sheet, with its rating and note as typed
    val editingSteepId: String? = null,
    val editRating: Float = 0f,
    val editNotes: String = "",

    // Save as configuration dialog
    val showSaveConfigurationDialog: Boolean = false,
    val suggestedConfigurationLabel: String = "",

    // In-progress session conflict dialog
    val inProgressDialogState: InProgressDialogState? = null,
)
