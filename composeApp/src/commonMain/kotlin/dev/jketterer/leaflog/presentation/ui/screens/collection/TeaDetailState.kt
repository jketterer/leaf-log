package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences

data class TeaDetailState(
    val tea: Tea? = null,
    val teaType: TeaType? = null,
    val recentSessions: List<TeaSession> = emptyList(),
    val configurations: List<BrewingConfiguration> = emptyList(),
    val vessels: List<BrewingVessel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val sessionPendingDelete: String? = null,
    val userPreferences: UserPreferences = UserPreferences(),
)