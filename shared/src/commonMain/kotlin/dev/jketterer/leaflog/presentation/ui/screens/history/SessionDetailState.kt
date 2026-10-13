package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.presentation.ui.viewmodel.InProgressDialogState
import kotlin.time.Duration

data class SessionDetailState(
    val session: SessionWithSteeps? = null,
    val tea: Tea? = null,
    val teaType: TeaType? = null,
    val vessel: BrewingVessel? = null,
    val usedConfigurationLabel: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val showDeleteSteepConfirmation: Boolean = false,
    val steepToDelete: String? = null,
    val showAddSteepDialog: Boolean = false,
    val nextSteepDuration: Duration? = null,
    val nextSteepTemperature: Double? = null,

    // Save as configuration dialog
    val showSaveConfigurationDialog: Boolean = false,
    val suggestedConfigurationLabel: String = "",

    // User preferences
    val userPreferences: UserPreferences = UserPreferences(),

    // In-progress session conflict dialog
    val inProgressDialogState: InProgressDialogState? = null,
)