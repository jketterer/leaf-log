package dev.jketterer.leaflog.presentation.ui.screens.settings

import dev.jketterer.leaflog.domain.models.ImportSummary
import dev.jketterer.leaflog.domain.models.UserPreferences

data class SettingsState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    /** Calendar days since the last delivered export; null if there's never been one. */
    val daysSinceBackup: Int? = null,
    val isBackupOverdue: Boolean = false,
    val error: String? = null,
    /** Confirms something finished, shown like an error but not one. */
    val notice: String? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    /** An export waiting to be delivered, deleted once it is or isn't. */
    val exportFilePath: String? = null,
    // Cleared as each picker opens, so a recreated screen doesn't open it again
    val showExportPicker: Boolean = false,
    val showImportPicker: Boolean = false,
    /** A picked backup waiting for the user to confirm it. */
    val importPreview: ImportPreview? = null,
    val importResult: ImportSummary? = null,
)

data class ImportPreview(val filePath: String, val summary: ImportSummary)
