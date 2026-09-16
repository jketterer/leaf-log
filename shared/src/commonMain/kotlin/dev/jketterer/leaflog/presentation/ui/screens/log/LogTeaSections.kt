package dev.jketterer.leaflog.presentation.ui.screens.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Play
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaCard
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.Preset
import dev.jketterer.leaflog.presentation.ui.components.common.PresetChips
import dev.jketterer.leaflog.presentation.ui.components.common.TemperatureInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VolumeInputField
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Tea picker row - shows the selected tea, or a button to open the search dialog.
 */
@Composable
internal fun TeaSelectionSection(
    selectedTea: Tea?,
    teaTypeName: String,
    teaTypeColorHex: String?,
    errorMessage: String?,
    onSelectTeaClicked: () -> Unit,
    modifier: Modifier = Modifier,
    imageStorage: ImageStorage? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Tea *",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )

        if (selectedTea != null) {
            TeaCard(
                tea = selectedTea,
                teaTypeName = teaTypeName,
                teaTypeColorHex = teaTypeColorHex,
                onTeaClick = {},
                trailingContent = {
                    TextButton(onClick = onSelectTeaClicked) {
                        Text("Change")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                imageStorage = imageStorage,
            )
        } else {
            Button(
                onClick = onSelectTeaClicked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Select Tea")
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Placeholder shown while the brewing parameters are gated behind tea + vessel selection.
 */
@Composable
internal fun SelectTeaAndVesselPrompt(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = FeatherIcons.Coffee,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Text(
            text = "Select a tea and vessel",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "to set up your brew parameters",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
internal fun BrewingParametersHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "BREWING PARAMETERS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "* Required",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Loose leaf / tea bag toggle with the leaf weight input for loose leaf brews.
 */
@Composable
internal fun TeaQuantitySection(
    isTeaBag: Boolean,
    teaQuantityGrams: String,
    errorMessage: String?,
    onTeaBagModeChanged: (Boolean) -> Unit,
    onTeaQuantityChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val teaQtyPresets = remember {
        listOf("1", "2", "3", "4", "5", "7", "10").map { Preset("${it}g", it) }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { onTeaBagModeChanged(false) },
                selected = !isTeaBag,
                label = { Text("Loose Leaf") },
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { onTeaBagModeChanged(true) },
                selected = isTeaBag,
                label = { Text("Tea Bag") },
            )
        }
        if (!isTeaBag) {
            OutlinedTextField(
                value = teaQuantityGrams,
                onValueChange = onTeaQuantityChanged,
                label = { Text("Tea Quantity *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                suffix = { Text("g") },
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
            PresetChips(
                presets = teaQtyPresets,
                currentValue = teaQuantityGrams,
                isSelected = { preset, current ->
                    preset.toFloatOrNull() == current?.toFloatOrNull()
                },
                onSelect = onTeaQuantityChanged,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun BrewingTimeSection(
    brewingTime: Duration?,
    errorMessage: String?,
    onBrewingTimeChanged: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brewingTimePresets = remember {
        listOf(
            Preset("0:30", 30.seconds),
            Preset("0:45", 45.seconds),
            Preset("1:00", 1.minutes),
            Preset("1:30", 1.minutes + 30.seconds),
            Preset("2:00", 2.minutes),
            Preset("3:00", 3.minutes),
            Preset("4:00", 4.minutes),
            Preset("5:00", 5.minutes),
        )
    }

    Column(modifier = modifier) {
        DurationPicker(
            duration = brewingTime,
            onDurationChange = { duration ->
                duration?.let { onBrewingTimeChanged(it) }
            },
            label = "Brewing Time *",
            isError = errorMessage != null,
            errorMessage = errorMessage,
            modifier = Modifier.fillMaxWidth(),
        )
        PresetChips(
            presets = brewingTimePresets,
            currentValue = brewingTime,
            onSelect = onBrewingTimeChanged,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun TemperatureSection(
    displayValue: String,
    unit: TemperatureUnit,
    errorMessage: String?,
    onTemperatureChanged: (String) -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tempPresets = remember(unit) {
        when (unit) {
            TemperatureUnit.CELSIUS -> listOf(60, 70, 75, 80, 85, 90, 95, 100)
            TemperatureUnit.FAHRENHEIT -> listOf(140, 160, 170, 175, 185, 195, 200, 208, 212)
        }.map { Preset("$it${unit.symbol}", it.toString()) }
    }

    Column(modifier = modifier) {
        TemperatureInputField(
            value = displayValue,
            onValueChange = onTemperatureChanged,
            currentUnit = unit,
            onToggleUnit = onToggleUnit,
            label = { Text("Temperature *") },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        PresetChips(
            presets = tempPresets,
            currentValue = displayValue,
            isSelected = { preset, current ->
                preset.toFloatOrNull() == current?.toFloatOrNull()
            },
            onSelect = onTemperatureChanged,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun WaterQuantitySection(
    displayValue: String,
    unit: VolumeUnit,
    errorMessage: String?,
    onWaterQuantityChanged: (String) -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val waterPresets = remember(unit) {
        when (unit) {
            VolumeUnit.MILLILITERS -> listOf(100, 150, 200, 250, 300, 400, 500)
            VolumeUnit.FLUID_OUNCES -> listOf(4, 6, 8, 10, 12, 16)
        }.map { Preset("$it ${unit.symbol}", it.toString()) }
    }

    Column(modifier = modifier) {
        VolumeInputField(
            value = displayValue,
            onValueChange = onWaterQuantityChanged,
            currentUnit = unit,
            onToggleUnit = onToggleUnit,
            label = { Text("Water Quantity *") },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        PresetChips(
            presets = waterPresets,
            currentValue = displayValue,
            isSelected = { preset, current ->
                preset.toFloatOrNull() == current?.toFloatOrNull()
            },
            onSelect = onWaterQuantityChanged,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Collapsible section for water type and location, summarising its contents when collapsed.
 */
@Composable
internal fun OptionalDetailsSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    summary: String,
    selectedWaterType: WaterType,
    location: String,
    onWaterTypeSelected: (WaterType) -> Unit,
    onLocationChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPTIONAL DETAILS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (!expanded && summary.isNotEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) FeatherIcons.ChevronUp else FeatherIcons.ChevronDown,
                contentDescription = if (expanded) "Collapse optional fields" else "Expand optional fields",
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(modifier = Modifier.height(0.dp))

                WaterTypeSelector(
                    selectedWaterType = selectedWaterType,
                    onWaterTypeSelected = onWaterTypeSelected,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChanged,
                    label = { Text("Location (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }
        }
    }
}

/**
 * Bottom action row - log the session as already brewed, or start the timer.
 */
@Composable
internal fun LogTeaActionButtons(
    onLogSessionClicked: () -> Unit,
    onBeginSessionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onLogSessionClicked,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                FeatherIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Session")
        }
        Button(
            onClick = onBeginSessionClicked,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                FeatherIcons.Play,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Begin Session")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaSelectionSectionPreview() {
    LeafLogTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TeaSelectionSection(
                selectedTea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    totalSessions = 12,
                    averageRating = 4.2f,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                teaTypeName = "Green",
                teaTypeColorHex = "#4CAF50",
                errorMessage = null,
                onSelectTeaClicked = {},
            )
            TeaSelectionSection(
                selectedTea = null,
                teaTypeName = "",
                teaTypeColorHex = null,
                errorMessage = "Please select a tea",
                onSelectTeaClicked = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingParameterSectionsPreview() {
    LeafLogTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BrewingParametersHeader()
            TeaQuantitySection(
                isTeaBag = false,
                teaQuantityGrams = "5",
                errorMessage = null,
                onTeaBagModeChanged = {},
                onTeaQuantityChanged = {},
            )
            BrewingTimeSection(
                brewingTime = 2.minutes + 30.seconds,
                errorMessage = null,
                onBrewingTimeChanged = {},
            )
            TemperatureSection(
                displayValue = "80",
                unit = TemperatureUnit.CELSIUS,
                errorMessage = null,
                onTemperatureChanged = {},
                onToggleUnit = {},
            )
            WaterQuantitySection(
                displayValue = "200",
                unit = VolumeUnit.MILLILITERS,
                errorMessage = null,
                onWaterQuantityChanged = {},
                onToggleUnit = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OptionalDetailsSectionPreview() {
    LeafLogTheme {
        OptionalDetailsSection(
            expanded = true,
            onExpandedChange = {},
            summary = "Spring · Kitchen",
            selectedWaterType = WaterType.SPRING,
            location = "Kitchen",
            onWaterTypeSelected = {},
            onLocationChanged = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LogTeaActionButtonsPreview() {
    LeafLogTheme {
        LogTeaActionButtons(
            onLogSessionClicked = {},
            onBeginSessionClicked = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectTeaAndVesselPromptPreview() {
    LeafLogTheme {
        SelectTeaAndVesselPrompt()
    }
}
