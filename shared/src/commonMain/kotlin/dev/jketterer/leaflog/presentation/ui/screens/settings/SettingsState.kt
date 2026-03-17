package dev.jketterer.leaflog.presentation.ui.screens.settings

import dev.jketterer.leaflog.domain.models.ImportResult
import dev.jketterer.leaflog.domain.models.UserPreferences

data class SettingsState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportFilePath: String? = null,
    val showImportPicker: Boolean = false,
    val importResult: ImportResult? = null,
)
