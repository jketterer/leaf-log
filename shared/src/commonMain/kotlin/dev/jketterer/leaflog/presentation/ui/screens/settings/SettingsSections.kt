package dev.jketterer.leaflog.presentation.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.ThemeMode
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.header_brewing
import leaf_log.shared.generated.resources.header_data
import leaf_log.shared.generated.resources.header_management
import leaf_log.shared.generated.resources.header_notifications
import leaf_log.shared.generated.resources.settings_appearance
import leaf_log.shared.generated.resources.settings_theme
import leaf_log.shared.generated.resources.theme_mode_dark
import leaf_log.shared.generated.resources.theme_mode_light
import leaf_log.shared.generated.resources.theme_mode_system
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppearanceSection(
    themeMode: ThemeMode,
    temperatureUnit: TemperatureUnit,
    volumeUnit: VolumeUnit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
    onVolumeUnitChange: (VolumeUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(Res.string.settings_appearance),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        ThemeModeSetting(
            currentMode = themeMode,
            onModeChange = onThemeModeChange,
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        TemperatureUnitSetting(
            currentUnit = temperatureUnit,
            onUnitChange = onTemperatureUnitChange,
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        VolumeUnitSetting(
            currentUnit = volumeUnit,
            onUnitChange = onVolumeUnitChange,
        )
    }
}

@Composable
internal fun BrewingSection(
    defaultWaterType: WaterType,
    onDefaultWaterTypeChange: (WaterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(Res.string.header_brewing),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        DefaultWaterTypeSetting(
            currentWaterType = defaultWaterType,
            onWaterTypeChange = onDefaultWaterTypeChange,
        )
    }
}

@Composable
internal fun NotificationsSection(
    timerCompletionEnabled: Boolean,
    sessionReminderEnabled: Boolean,
    onTimerCompletionChange: (Boolean) -> Unit,
    onSessionReminderChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader(
            title = stringResource(Res.string.header_notifications),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        SettingRow(
            title = "Brew complete",
            subtitle = "Alert when the steep timer finishes. Turning this off means no alert when your tea is ready.",
        ) {
            Switch(
                checked = timerCompletionEnabled,
                onCheckedChange = onTimerCompletionChange,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SettingRow(
            title = "Finish your session",
            subtitle = "Remind you to rate and finish a session you left open, shortly after the tea has cooled.",
        ) {
            Switch(
                checked = sessionReminderEnabled,
                onCheckedChange = onSessionReminderChange,
            )
        }
    }
}

@Composable
internal fun ManagementSection(
    onNavigateToTeaTypes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader(
            title = stringResource(Res.string.header_management),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

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
internal fun DataSection(
    daysSinceBackup: Int?,
    isBackupOverdue: Boolean,
    isExporting: Boolean,
    isImporting: Boolean,
    photoStorage: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader(
            title = stringResource(Res.string.header_data),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        SettingRow(
            title = "Export Data",
            subtitle = "Save all data including photos",
            status = "Last backup: " + when (daysSinceBackup) {
                null -> "never"
                0 -> "today"
                1 -> "yesterday"
                else -> "$daysSinceBackup days ago"
            },
            statusColor = if (isBackupOverdue) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            OutlinedButton(
                onClick = onExport,
                enabled = !isExporting,
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (isBackupOverdue) "Back up now" else "Export")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SettingRow(
            title = "Import Data",
            subtitle = "Merge data from a Leaf Log export",
        ) {
            OutlinedButton(
                onClick = onImport,
                enabled = !isImporting,
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Import")
                }
            }
        }

        photoStorage?.let { size ->
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingRow(
                title = "Photos",
                subtitle = "Space your photos take on this device",
            ) {
                Text(
                    text = size,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSetting(
    currentMode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_theme),
            style = MaterialTheme.typography.bodyLarge,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == currentMode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.entries.size,
                    ),
                ) {
                    Text(
                        stringResource(
                            when (mode) {
                                ThemeMode.SYSTEM -> Res.string.theme_mode_system
                                ThemeMode.LIGHT -> Res.string.theme_mode_light
                                ThemeMode.DARK -> Res.string.theme_mode_dark
                            },
                        ),
                    )
                }
            }
        }
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
    status: String? = null,
    statusColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            status?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                )
            }
        }
        content()
    }
}

// Previews
@Composable
private fun PreviewDataSection(
    daysSinceBackup: Int? = null,
    isBackupOverdue: Boolean = false,
    isExporting: Boolean = false,
    photoStorage: String? = null,
) {
    DataSection(
        daysSinceBackup = daysSinceBackup,
        isBackupOverdue = isBackupOverdue,
        isExporting = isExporting,
        isImporting = false,
        photoStorage = photoStorage,
        onExport = {},
        onImport = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun DataSectionPreview() {
    LeafLogTheme {
        PreviewDataSection()
    }
}

@Preview(showBackground = true)
@Composable
private fun DataSectionBackedUpPreview() {
    LeafLogTheme {
        PreviewDataSection(daysSinceBackup = 3, photoStorage = "84 MB")
    }
}

@Preview(showBackground = true)
@Composable
private fun DataSectionStalePreview() {
    LeafLogTheme {
        PreviewDataSection(daysSinceBackup = 42, isBackupOverdue = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun DataSectionExportingPreview() {
    LeafLogTheme {
        PreviewDataSection(isExporting = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun AppearanceSectionPreview() {
    LeafLogTheme {
        AppearanceSection(
            themeMode = ThemeMode.DARK,
            temperatureUnit = TemperatureUnit.CELSIUS,
            volumeUnit = VolumeUnit.MILLILITERS,
            onThemeModeChange = {},
            onTemperatureUnitChange = {},
            onVolumeUnitChange = {},
        )
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
            timerCompletionEnabled = true,
            sessionReminderEnabled = false,
            onTimerCompletionChange = {},
            onSessionReminderChange = {},
        )
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
