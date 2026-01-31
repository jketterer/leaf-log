package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel

data class VesselListState(
    val vessels: List<BrewingVessel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
