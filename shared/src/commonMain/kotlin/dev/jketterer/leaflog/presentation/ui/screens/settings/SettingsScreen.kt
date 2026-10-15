package dev.jketterer.leaflog.presentation.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.ImportSummary
import dev.jketterer.leaflog.domain.models.RelativeDay
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.relativeDayOf
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import androidx.compose.material3.IconButton
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ChevronRight
import dev.jketterer.leaflog.presentation.ui.components.common.DialogPreviewContainer
import dev.jketterer.leaflog.presentation.ui.components.common.ExportFileEffect
import dev.jketterer.leaflog.presentation.ui.components.common.ImportFileLauncher
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTeaTypes: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    SettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
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
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                }
            },
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

            NotificationsSection(
                state = state,
                onIntent = onIntent
            )

            ManagementSection(
                onNavigateToTeaTypes = onNavigateToTeaTypes,
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
        exportFilePath = state.exportFilePath,
        fileName = remember {
            val now = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val date = now.date
            val time = now.time
            val month = (date.month.ordinal + 1).toString().padStart(2, '0')
            val day = date.day.toString().padStart(2, '0')
            val hour = time.hour.toString().padStart(2, '0')
            val minute = time.minute.toString().padStart(2, '0')
            val second = time.second.toString().padStart(2, '0')
            "leaf-log-export-${date.year}${month}${day}-${hour}${minute}${second}.zip"
        },
        onExported = { onIntent(SettingsIntent.ExportCompleted) },
        onError = { message -> onIntent(SettingsIntent.ExportFailed(message)) },
    )

    ImportFileLauncher(
        shouldLaunch = state.showImportPicker,
        onFilePath = { path -> onIntent(SettingsIntent.ImportFileSelected(path)) },
        onCancelled = { onIntent(SettingsIntent.ImportCancelled) },
        onError = { message -> onIntent(SettingsIntent.ImportFailed(message)) },
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

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        DefaultWaterTypeSetting(
            currentWaterType = state.preferences.defaultWaterType,
            onWaterTypeChange = { onIntent(SettingsIntent.UpdateDefaultWaterType(it)) }
        )
    }
}

@Composable
private fun NotificationsSection(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader(title = "Notifications")

        SettingRow(
            title = "Brew complete",
            subtitle = "Alert when the steep timer finishes. Turning this off means no alert when your tea is ready.",
        ) {
            Switch(
                checked = state.preferences.timerCompletionNotificationsEnabled,
                onCheckedChange = {
                    onIntent(SettingsIntent.UpdateTimerCompletionNotifications(it))
                },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SettingRow(
            title = "Finish your session",
            subtitle = "Remind you to rate and finish a session you left open, shortly after the tea has cooled.",
        ) {
            Switch(
                checked = state.preferences.sessionReminderNotificationsEnabled,
                onCheckedChange = {
                    onIntent(SettingsIntent.UpdateSessionReminderNotifications(it))
                },
            )
        }
    }
}

@Composable
private fun ManagementSection(
    onNavigateToTeaTypes: () -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader(title = "Management")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToTeaTypes)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Tea Types",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Manage tea categories and defaults",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = FeatherIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            subtitle = "Save all data including photos",
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
            subtitle = "Merge data from a Leaf Log export",
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
private fun ImportPreviewDialog(
    summary: ImportSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import this backup?") },
        text = { ImportSummaryContent(summary, isPreview = true) },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !summary.changesNothing) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ImportResultDialog(
    result: ImportSummary,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Complete") },
        text = { ImportSummaryContent(result, isPreview = false) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

@Composable
private fun ImportSummaryContent(summary: ImportSummary, isPreview: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isPreview) {
            val made = TimeFormatter.formatRelativeTimestamp(summary.exportedAt)
            val day = relativeDayOf(
                summary.exportedAt,
                Clock.System.now(),
                TimeZone.currentSystemDefault(),
            )
            val date = when (day) {
                RelativeDay.TODAY -> "today at $made"
                RelativeDay.YESTERDAY -> "yesterday"
                else -> made
            }
            val emphasis = SpanStyle(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = buildAnnotatedString {
                    append("Backed up ")
                    withStyle(emphasis) { append(date) }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (summary.changesNothing) {
            Text(
                text = "This device already has everything in the backup.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            // The counts share the widest one's width, so their labels line up
            val counts = listOf(summary.added, summary.updated, summary.removed)
            val countStyle = MaterialTheme.typography.titleLarge
                .copy(fontWeight = FontWeight.SemiBold)
            val measurer = rememberTextMeasurer()
            val countWidth = with(LocalDensity.current) {
                counts.maxOf { measurer.measure(it.toString(), countStyle).size.width }.toDp()
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ImportStat(summary.added, "new", countWidth, countStyle)
                ImportStat(summary.updated, "updated", countWidth, countStyle)
                ImportStat(
                    summary.removed,
                    "removed",
                    countWidth,
                    countStyle,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (summary.restoresSettings) {
                Text(
                    text = if (isPreview) {
                        "Your settings come with it."
                    } else {
                        "Your settings were restored."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "Everything else stays as it is.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (summary.skipped > 0) {
            Text(
                text = "${summary.skipped} records are left out: what they belonged to was " +
                    "deleted before this backup was made.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A count beside its label, both dimmed when there's nothing to count. */
@Composable
private fun ImportStat(
    count: Int,
    label: String,
    countWidth: Dp,
    countStyle: TextStyle,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant
    Row {
        Text(
            text = count.toString(),
            style = countStyle,
            color = if (count == 0) dimmed else color,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(countWidth)
                .alignByBaseline(),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (count == 0) dimmed else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun DefaultWaterTypeSetting(
    currentWaterType: WaterType,
    onWaterTypeChange: (WaterType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Default Water Type",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Pre-selected water type when logging a session",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WaterTypeSelector(
            selectedWaterType = currentWaterType,
            onWaterTypeSelected = onWaterTypeChange,
        )
    }
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

private val previewSummary = ImportSummary(
    exportedAt = Instant.parse("2026-09-02T09:00:00Z"),
    added = 12,
    updated = 3,
    removed = 1,
    skipped = 3,
    restoresSettings = true,
)

@Preview(showBackground = true)
@Composable
private fun ImportPreviewDialogPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Import this backup?",
            buttons = {
                TextButton(onClick = {}) { Text("Cancel") }
                Button(onClick = {}) { Text("Import") }
            },
        ) {
            ImportSummaryContent(previewSummary, isPreview = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportPreviewDialogNothingNewPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Import this backup?",
            buttons = {
                TextButton(onClick = {}) { Text("Cancel") }
                Button(onClick = {}, enabled = false) { Text("Import") }
            },
        ) {
            ImportSummaryContent(
                previewSummary.copy(
                    added = 0,
                    updated = 0,
                    removed = 0,
                    skipped = 0,
                    restoresSettings = false,
                ),
                isPreview = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportResultDialogPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Import Complete",
            buttons = { Button(onClick = {}) { Text("OK") } },
        ) {
            ImportSummaryContent(previewSummary, isPreview = false)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultWaterTypeSettingPreview() {
    LeafLogTheme {
        DefaultWaterTypeSetting(
            currentWaterType = WaterType.FILTERED,
            onWaterTypeChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsSectionPreview() {
    LeafLogTheme {
        NotificationsSection(
            state = SettingsState(
                preferences = UserPreferences(
                    timerCompletionNotificationsEnabled = true,
                    sessionReminderNotificationsEnabled = false,
                ),
            ),
            onIntent = {},
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
