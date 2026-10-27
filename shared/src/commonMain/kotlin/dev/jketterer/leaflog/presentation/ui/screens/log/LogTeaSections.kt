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
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.WeightUnit
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaCard
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.LeafQuantityField
import dev.jketterer.leaflog.presentation.ui.components.common.Preset
import dev.jketterer.leaflog.presentation.ui.components.common.PresetChips
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.RinseSwitch
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.UnitInputField
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_tea_quantity_required
import leaf_log.shared.generated.resources.header_brewing_parameters
import leaf_log.shared.generated.resources.header_optional_details
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import leaf_log.shared.generated.resources.begin_session
import leaf_log.shared.generated.resources.log_session
import leaf_log.shared.generated.resources.select_tea

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
                Text(stringResource(Res.string.select_tea))
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
        SectionHeader(stringResource(Res.string.header_brewing_parameters))
        Text(
            text = "* Required",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

/**
 * Collapsible section for water type and location, summarizing its contents when collapsed.
 */
@Composable
internal fun OptionalDetailsSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    summary: String,
    selectedWaterType: WaterType,
    rinsed: Boolean,
    location: String,
    onRinsedChange: (Boolean) -> Unit,
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
                SectionHeader(stringResource(Res.string.header_optional_details))
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

                RinseSwitch(checked = rinsed, onCheckedChange = onRinsedChange)

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
            Text(stringResource(Res.string.log_session))
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
            Text(stringResource(Res.string.begin_session))
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
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
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
            LeafQuantityField(
                isTeaBag = false,
                teaQuantityGrams = 5.0,
                onTeaBagModeChanged = {},
                onTeaQuantityChanged = {},
                label = stringResource(Res.string.field_tea_quantity_required),
                presets = true,
                weightUnit = WeightUnit.GRAMS,
            )
            BrewingTimeSection(
                brewingTime = 2.minutes + 30.seconds,
                errorMessage = null,
                onBrewingTimeChanged = {},
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
            rinsed = true,
            location = "Kitchen",
            onRinsedChange = {},
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
