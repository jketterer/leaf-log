package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.UserPreferences

data class VesselListState(
    val vessels: List<BrewingVessel> = emptyList(),
    val userPreferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
