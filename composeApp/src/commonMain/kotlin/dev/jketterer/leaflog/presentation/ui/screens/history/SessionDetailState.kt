package dev.jketterer.leaflog.presentation.ui.screens.history

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.UserPreferences

data class SessionDetailState(
    val parentSession: TeaSession? = null,
    val childSteeps: List<TeaSession> = emptyList(),
    val tea: Tea? = null,
    val teaType: TeaType? = null,
    val vessel: BrewingVessel? = null,

    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val showDeleteSteepConfirmation: Boolean = false,
    val steepToDelete: String? = null,

    // User preferences
    val userPreferences: UserPreferences = UserPreferences(),
) {
    val allSteeps: List<TeaSession>
        get() = if (parentSession != null) {
            listOf(parentSession) + childSteeps.sortedBy { it.steepNumber }
        } else {
            emptyList()
        }

    val isSingleSteep: Boolean
        get() = childSteeps.isEmpty()

    val isMultiSteep: Boolean
        get() = childSteeps.isNotEmpty()
}