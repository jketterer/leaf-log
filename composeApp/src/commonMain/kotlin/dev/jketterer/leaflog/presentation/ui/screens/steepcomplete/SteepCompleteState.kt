package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.UserPreferences
import kotlin.time.Duration

data class SteepCompleteState(
    val session: TeaSession? = null,
    val tea: Tea? = null,
    val vessel: BrewingVessel? = null,
    val rating: Float = 0f,
    val notes: String = "",
    val photos: List<String> = emptyList(),
    val previousSteeps: List<TeaSession> = emptyList(),
    // Next steep dialog
    val showNextSteepDialog: Boolean = false,
    val nextSteepDuration: Duration? = null,
    val nextSteepTemperature: Double? = null,
    val nextSteepWaterQuantity: Double? = null,
    // Config save dialog
    val showSaveConfigurationDialog: Boolean = false,
    val suggestedConfigurationLabel: String = "",
    val savedSession: TeaSession? = null,
    // Discard
    val showDiscardConfirmation: Boolean = false,
    // Loading/error
    val isLoading: Boolean = true,
    val error: String? = null,
    val userPreferences: UserPreferences = UserPreferences(),
)
