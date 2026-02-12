package dev.jketterer.leaflog.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.ImportResult
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.presentation.ui.components.common.ExportFileEffect
import dev.jketterer.leaflog.presentation.ui.components.common.ImportFileLauncher
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    SettingsContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            PreferencesSection(
                state = state,
                onIntent = onIntent
            )

            DataSection(
                state = state,
                onIntent = onIntent
            )
        }

        // Error snackbar
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { onIntent(SettingsIntent.ClearError) }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }

    // File operation effects
    ExportFileEffect(
        jsonContent = state.exportJson,
        fileName = "leaf-log-export.json",
        onExported = { onIntent(SettingsIntent.ExportCompleted) },
        onError = { error ->
            onIntent(SettingsIntent.ExportCompleted)
            onIntent(SettingsIntent.ClearError) // Clear any prior, then set new
        },
    )

    ImportFileLauncher(
        shouldLaunch = state.showImportPicker,
        onFileContent = { content -> onIntent(SettingsIntent.ImportFileSelected(content)) },
        onCancelled = { onIntent(SettingsIntent.ImportCancelled) },
        onError = { onIntent(SettingsIntent.ImportCancelled) },
    )

    // Import result dialog
    state.importResult?.let { result ->
        ImportResultDialog(
            result = result,
            onDismiss = { onIntent(SettingsIntent.DismissImportResult) },
        )
    }
}

@Composable
private fun PreferencesSection(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit
) {
    Column {
        SectionHeader(title = "Preferences")

        TemperatureUnitSetting(
            currentUnit = state.preferences.temperatureUnit,
            onUnitChange = { onIntent(SettingsIntent.UpdateTemperatureUnit(it)) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        VolumeUnitSetting(
            currentUnit = state.preferences.volumeUnit,
            onUnitChange = { onIntent(SettingsIntent.UpdateVolumeUnit(it)) }
        )
    }
}

@Composable
private fun DataSection(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader(title = "Data")

        SettingRow(
            title = "Export Data",
            subtitle = "Save all data as a JSON file",
        ) {
            OutlinedButton(
                onClick = { onIntent(SettingsIntent.ExportData) },
                enabled = !state.isExporting,
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Export")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SettingRow(
            title = "Import Data",
            subtitle = "Merge data from a Leaf Log export file",
        ) {
            OutlinedButton(
                onClick = { onIntent(SettingsIntent.ImportData) },
                enabled = !state.isImporting,
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Import")
                }
            }
        }
    }
}

@Composable
private fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Complete") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Successfully imported:")
                Text("  ${result.teaTypesImported} tea types")
                Text("  ${result.brewingVesselsImported} brewing vessels")
                Text("  ${result.teasImported} teas")
                Text("  ${result.teaSessionsImported} tea sessions")
                Text("  ${result.configurationsImported} brewing configurations")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun TemperatureUnitSetting(
    currentUnit: TemperatureUnit,
    onUnitChange: (TemperatureUnit) -> Unit
) {
    SettingRow(
        title = "Temperature Unit",
        subtitle = "Choose display unit for temperatures"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TemperatureUnit.entries.forEach { unit ->
                FilterChip(
                    selected = unit == currentUnit,
                    onClick = { onUnitChange(unit) },
                    label = { Text(unit.symbol) }
                )
            }
        }
    }
}

@Composable
private fun VolumeUnitSetting(
    currentUnit: VolumeUnit,
    onUnitChange: (VolumeUnit) -> Unit
) {
    SettingRow(
        title = "Volume Unit",
        subtitle = "Choose display unit for volumes"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VolumeUnit.entries.forEach { unit ->
                FilterChip(
                    selected = unit == currentUnit,
                    onClick = { onUnitChange(unit) },
                    label = { Text(unit.symbol) }
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
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

@Preview(showBackground = true)
@Composable
private fun DataSectionPreview() {
    LeafLogTheme {
        DataSection(
            state = SettingsState(isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DataSectionExportingPreview() {
    LeafLogTheme {
        DataSection(
            state = SettingsState(isLoading = false, isExporting = true),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportResultDialogPreview() {
    LeafLogTheme {
        ImportResultDialog(
            result = ImportResult(
                teaTypesImported = 6,
                brewingVesselsImported = 4,
                teasImported = 12,
                teaSessionsImported = 45,
                configurationsImported = 8,
            ),
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    LeafLogTheme {
        SectionHeader(title = "Preferences")
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingRowPreview() {
    LeafLogTheme {
        SettingRow(
            title = "Sample Setting",
            subtitle = "This is a sample setting description"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("Option 1") }
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Option 2") }
                )
            }
        }
    }
}
