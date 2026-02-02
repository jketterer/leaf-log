package dev.jketterer.leaflog.presentation.ui.screens.settings

import dev.jketterer.leaflog.domain.models.UserPreferences

data class SettingsState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val error: String? = null
)
