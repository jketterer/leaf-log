package dev.jketterer.leaflog.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.presentation.ui.components.backup.ImportPreviewDialog
import dev.jketterer.leaflog.presentation.ui.components.backup.ImportResultDialog
import dev.jketterer.leaflog.presentation.ui.components.common.ExportFileEffect
import dev.jketterer.leaflog.presentation.ui.components.common.ImportFileLauncher
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTeaTypes: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    SettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onNavigateToTeaTypes = onNavigateToTeaTypes,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToTeaTypes: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            AppearanceSection(
                themeMode = state.preferences.themeMode,
                temperatureUnit = state.preferences.temperatureUnit,
                volumeUnit = state.preferences.volumeUnit,
                onThemeModeChange = { onIntent(SettingsIntent.UpdateThemeMode(it)) },
                onTemperatureUnitChange = { onIntent(SettingsIntent.UpdateTemperatureUnit(it)) },
                onVolumeUnitChange = { onIntent(SettingsIntent.UpdateVolumeUnit(it)) },
            )

            BrewingSection(
                defaultWaterType = state.preferences.defaultWaterType,
                onDefaultWaterTypeChange = { onIntent(SettingsIntent.UpdateDefaultWaterType(it)) },
            )

            NotificationsSection(
                timerCompletionEnabled = state.preferences.timerCompletionNotificationsEnabled,
                sessionReminderEnabled = state.preferences.sessionReminderNotificationsEnabled,
                onTimerCompletionChange = {
                    onIntent(SettingsIntent.UpdateTimerCompletionNotifications(it))
                },
                onSessionReminderChange = {
                    onIntent(SettingsIntent.UpdateSessionReminderNotifications(it))
                },
            )

            ManagementSection(
                onNavigateToTeaTypes = onNavigateToTeaTypes,
            )

            DataSection(
                daysSinceBackup = state.daysSinceBackup,
                isBackupOverdue = state.isBackupOverdue,
                isExporting = state.isExporting,
                isImporting = state.isImporting,
                photoStorage = state.photoStorage,
                onExport = { onIntent(SettingsIntent.ExportData) },
                onImport = { onIntent(SettingsIntent.ImportData) },
            )
        }
    }

    SettingsDialogs(
        state = state,
        onIntent = onIntent,
    )
}

/** The file pickers and the import dialogs. */
@Composable
private fun SettingsDialogs(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    ExportFileEffect(
        exportFilePath = state.exportFilePath,
        shouldLaunch = state.showExportPicker,
        onLaunched = { onIntent(SettingsIntent.ExportPickerShown) },
        onExported = { onIntent(SettingsIntent.ExportCompleted) },
        onCancelled = { onIntent(SettingsIntent.ExportCancelled) },
        onError = { onIntent(SettingsIntent.ExportFailed) },
    )

    ImportFileLauncher(
        shouldLaunch = state.showImportPicker,
        onLaunched = { onIntent(SettingsIntent.ImportPickerShown) },
        onFilePath = { path -> onIntent(SettingsIntent.ImportFileSelected(path)) },
        onCancelled = { onIntent(SettingsIntent.ImportCancelled) },
        onError = { onIntent(SettingsIntent.ImportFailed) },
    )

    state.importPreview?.let { preview ->
        ImportPreviewDialog(
            summary = preview.summary,
            onConfirm = { onIntent(SettingsIntent.ConfirmImport) },
            onDismiss = { onIntent(SettingsIntent.CancelImport) },
        )
    }

    state.importResult?.let { result ->
        ImportResultDialog(
            result = result,
            onDismiss = { onIntent(SettingsIntent.DismissImportResult) },
        )
    }
}

// Previews
@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    LeafLogTheme {
        SettingsContent(
            state = SettingsState(
                preferences = UserPreferences(
                    temperatureUnit = TemperatureUnit.CELSIUS,
                    volumeUnit = VolumeUnit.MILLILITERS
                ),
                isLoading = false
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenFahrenheitPreview() {
    LeafLogTheme {
        SettingsContent(
            state = SettingsState(
                preferences = UserPreferences(
                    temperatureUnit = TemperatureUnit.FAHRENHEIT,
                    volumeUnit = VolumeUnit.FLUID_OUNCES
                ),
                isLoading = false
            ),
            onIntent = {}
        )
    }
}
