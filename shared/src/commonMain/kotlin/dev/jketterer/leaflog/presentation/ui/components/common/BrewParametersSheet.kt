package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.WeightUnit
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselChipRow
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.discard
import leaf_log.shared.generated.resources.discard_changes_text
import leaf_log.shared.generated.resources.discard_changes_title
import leaf_log.shared.generated.resources.edit_parameters
import leaf_log.shared.generated.resources.field_temperature
import leaf_log.shared.generated.resources.field_water_quantity
import leaf_log.shared.generated.resources.field_water_type
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.save
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A brew's parameters, changed in place. Save sits in the header, where no keyboard reaches it,
 * and closing the sheet with [hasChanges] asks before they're lost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewParametersSheet(
    temperatureCelsius: Double?,
    waterQuantityMl: Double?,
    hasChanges: Boolean,
    onTemperatureChanged: (Double?) -> Unit,
    onWaterQuantityChanged: (Double?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    brewingTime: Duration? = null,
    onBrewingTimeChanged: ((Duration) -> Unit)? = null,
    // The vessel row shows only when [vessels] is given
    vessels: List<BrewingVessel>? = null,
    selectedVessel: BrewingVessel? = null,
    onVesselSelected: (BrewingVessel) -> Unit = {},
    // The leaf row shows only when [isTeaBag] is given, the water type only when [waterType] is
    teaQuantityGrams: Double? = null,
    isTeaBag: Boolean? = null,
    onTeaQuantityChanged: (Double?) -> Unit = {},
    onTeaBagModeChanged: (Boolean) -> Unit = {},
    waterType: WaterType? = null,
    onWaterTypeSelected: (WaterType) -> Unit = {},
    temperatureUnit: TemperatureUnit = LocalUnits.current.temperature,
    volumeUnit: VolumeUnit = LocalUnits.current.volume,
    weightUnit: WeightUnit = LocalUnits.current.weight,
) {
    var confirmingDiscard by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberExpandedSheetState(
        confirmHide = {
            if (hasChanges) confirmingDiscard = true
            !hasChanges
        },
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        BrewParametersContent(
            temperatureCelsius = temperatureCelsius,
            waterQuantityMl = waterQuantityMl,
            teaQuantityGrams = teaQuantityGrams,
            isTeaBag = isTeaBag,
            waterType = waterType,
            temperatureUnit = temperatureUnit,
            volumeUnit = volumeUnit,
            weightUnit = weightUnit,
            onTemperatureChanged = onTemperatureChanged,
            onWaterQuantityChanged = onWaterQuantityChanged,
            onTeaQuantityChanged = onTeaQuantityChanged,
            onTeaBagModeChanged = onTeaBagModeChanged,
            onWaterTypeSelected = onWaterTypeSelected,
            onSave = onSave,
            brewingTime = brewingTime,
            onBrewingTimeChanged = onBrewingTimeChanged,
            vessels = vessels,
            selectedVessel = selectedVessel,
            onVesselSelected = onVesselSelected,
        )

        if (confirmingDiscard) {
            ConfirmDestructiveDialog(
                title = stringResource(Res.string.discard_changes_title),
                text = stringResource(Res.string.discard_changes_text),
                confirmLabel = stringResource(Res.string.discard),
                onConfirm = {
                    confirmingDiscard = false
                    onDismiss()
                },
                onDismiss = { confirmingDiscard = false },
            )
        }
    }
}

@Composable
internal fun BrewParametersContent(
    temperatureCelsius: Double?,
    waterQuantityMl: Double?,
    onTemperatureChanged: (Double?) -> Unit,
    onWaterQuantityChanged: (Double?) -> Unit,
    onSave: () -> Unit,
    // An arrow before the title when given, for parameters opened from within another sheet
    onBack: (() -> Unit)? = null,
    brewingTime: Duration? = null,
    onBrewingTimeChanged: ((Duration) -> Unit)? = null,
    // The vessel row shows only when [vessels] is given
    vessels: List<BrewingVessel>? = null,
    selectedVessel: BrewingVessel? = null,
    onVesselSelected: (BrewingVessel) -> Unit = {},
    // The leaf row shows only when [isTeaBag] is given, the water type only when [waterType] is
    teaQuantityGrams: Double? = null,
    isTeaBag: Boolean? = null,
    onTeaQuantityChanged: (Double?) -> Unit = {},
    onTeaBagModeChanged: (Boolean) -> Unit = {},
    waterType: WaterType? = null,
    onWaterTypeSelected: (WaterType) -> Unit = {},
    temperatureUnit: TemperatureUnit = LocalUnits.current.temperature,
    volumeUnit: VolumeUnit = LocalUnits.current.volume,
    weightUnit: WeightUnit = LocalUnits.current.weight,
) {
    val isValid = temperatureCelsius != null &&
            waterQuantityMl != null &&
            (isTeaBag != false || teaQuantityGrams != null) &&
            (brewingTime == null || brewingTime > Duration.ZERO)
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth().clearFocusOnTap()) {
        BrewParametersHeader(
            contentAbove = scrollState.canScrollBackward,
            isValid = isValid,
            onSave = onSave,
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (brewingTime != null) {
                DurationPicker(
                    duration = brewingTime.takeIf { it > Duration.ZERO },
                    onDurationChange = { onBrewingTimeChanged?.invoke(it ?: Duration.ZERO) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (vessels != null) {
                VesselChipRow(
                    vessels = vessels,
                    selectedVessel = selectedVessel,
                    onVesselSelected = onVesselSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Side by side, so more of the form stays above the keyboard
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UnitInputField(
                    value = temperatureCelsius,
                    onValueChange = onTemperatureChanged,
                    unit = temperatureUnit,
                    label = stringResource(Res.string.field_temperature),
                    modifier = Modifier.weight(1f),
                )
                UnitInputField(
                    value = waterQuantityMl,
                    onValueChange = onWaterQuantityChanged,
                    unit = volumeUnit,
                    label = stringResource(Res.string.field_water_quantity),
                    modifier = Modifier.weight(1f),
                )
            }

            if (isTeaBag != null) {
                LeafQuantityField(
                    isTeaBag = isTeaBag,
                    teaQuantityGrams = teaQuantityGrams,
                    onTeaBagModeChanged = onTeaBagModeChanged,
                    onTeaQuantityChanged = onTeaQuantityChanged,
                    weightUnit = weightUnit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (waterType != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(stringResource(Res.string.field_water_type))
                    WaterTypeChips(
                        selectedWaterType = waterType,
                        onWaterTypeSelected = onWaterTypeSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * The title and Save, pinned above the scrolling fields. The divider fades in only while fields
 * are scrolled away above; it keeps its space either way, so nothing shifts.
 */
@Composable
private fun BrewParametersHeader(
    contentAbove: Boolean,
    isValid: Boolean,
    onSave: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val dividerAlpha by animateFloatAsState(if (contentAbove) 1f else 0f)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // The arrow's own touch target stands in for the padding before it
                .padding(start = if (onBack != null) 12.dp else 24.dp, end = 24.dp)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = stringResource(Res.string.navigate_up))
                }
            }
            Text(
                text = stringResource(Res.string.edit_parameters),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSave, enabled = isValid) {
                Text(stringResource(Res.string.save))
            }
        }
        HorizontalDivider(modifier = Modifier.graphicsLayer { alpha = dividerAlpha })
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewParametersContentLooseLeafPreview() {
    LeafLogTheme {
        SheetPreviewContainer(height = 560.dp) {
            BrewParametersContent(
                temperatureCelsius = 80.0,
                waterQuantityMl = 200.0,
                teaQuantityGrams = 5.0,
                isTeaBag = false,
                waterType = WaterType.FILTERED,
                temperatureUnit = TemperatureUnit.CELSIUS,
                volumeUnit = VolumeUnit.MILLILITERS,
                weightUnit = WeightUnit.GRAMS,
                onTemperatureChanged = {},
                onWaterQuantityChanged = {},
                onTeaQuantityChanged = {},
                onTeaBagModeChanged = {},
                onWaterTypeSelected = {},
                onSave = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewParametersContentTeaBagPreview() {
    LeafLogTheme {
        SheetPreviewContainer(height = 480.dp) {
            BrewParametersContent(
                temperatureCelsius = 80.0,
                waterQuantityMl = 200.0,
                teaQuantityGrams = null,
                isTeaBag = true,
                waterType = WaterType.FILTERED,
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                volumeUnit = VolumeUnit.FLUID_OUNCES,
                weightUnit = WeightUnit.GRAMS,
                onTemperatureChanged = {},
                onWaterQuantityChanged = {},
                onTeaQuantityChanged = {},
                onTeaBagModeChanged = {},
                onWaterTypeSelected = {},
                onSave = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewParametersContentSteepPreview() {
    LeafLogTheme {
        SheetPreviewContainer(height = 360.dp) {
            BrewParametersContent(
                brewingTime = 25.seconds,
                temperatureCelsius = 95.0,
                waterQuantityMl = 120.0,
                onBrewingTimeChanged = {},
                onTemperatureChanged = {},
                onWaterQuantityChanged = {},
                onSave = {},
                onBack = {},
            )
        }
    }
}
