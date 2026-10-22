package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.WeightUnit
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.cancel
import leaf_log.shared.generated.resources.edit_parameters
import leaf_log.shared.generated.resources.field_temperature
import leaf_log.shared.generated.resources.field_water_quantity
import leaf_log.shared.generated.resources.save
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewParametersSheet(
    temperatureCelsius: Double?,
    waterQuantityMl: Double?,
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
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
            onDismiss = onDismiss,
            brewingTime = brewingTime,
            onBrewingTimeChanged = onBrewingTimeChanged,
            vessels = vessels,
            selectedVessel = selectedVessel,
            onVesselSelected = onVesselSelected,
        )
    }
}

@Composable
internal fun BrewParametersContent(
    temperatureCelsius: Double?,
    waterQuantityMl: Double?,
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
    val isValid = temperatureCelsius != null &&
            waterQuantityMl != null &&
            (isTeaBag != false || teaQuantityGrams != null) &&
            (brewingTime == null || brewingTime > Duration.ZERO)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(Res.string.edit_parameters),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Brewing Time (optional)
        if (brewingTime != null) {
            DurationPicker(
                duration = brewingTime.takeIf { it > Duration.ZERO },
                onDurationChange = { onBrewingTimeChanged?.invoke(it ?: Duration.ZERO) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (vessels != null) {
            VesselSelector(
                vessels = vessels,
                selectedVessel = selectedVessel,
                onVesselSelected = onVesselSelected,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        UnitInputField(
            value = temperatureCelsius,
            onValueChange = onTemperatureChanged,
            unit = temperatureUnit,
            label = stringResource(Res.string.field_temperature),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        UnitInputField(
            value = waterQuantityMl,
            onValueChange = onWaterQuantityChanged,
            unit = volumeUnit,
            label = stringResource(Res.string.field_water_quantity),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isTeaBag != null) {
            LeafQuantityField(
                isTeaBag = isTeaBag,
                teaQuantityGrams = teaQuantityGrams,
                onTeaBagModeChanged = onTeaBagModeChanged,
                onTeaQuantityChanged = onTeaQuantityChanged,
                weightUnit = weightUnit,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (waterType != null) {
            WaterTypeSelector(
                selectedWaterType = waterType,
                onWaterTypeSelected = onWaterTypeSelected,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.cancel))
            }

            Button(
                onClick = onSave,
                enabled = isValid,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.save))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewParametersContentLooseLeafPreview() {
    LeafLogTheme {
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
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewParametersContentTeaBagPreview() {
    LeafLogTheme {
        BrewParametersContent(
            temperatureCelsius = 80.0,
            waterQuantityMl = 200.0,
            teaQuantityGrams = null,
            isTeaBag = true,
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
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewParametersContentSteepPreview() {
    LeafLogTheme {
        BrewParametersContent(
            brewingTime = 25.seconds,
            temperatureCelsius = 95.0,
            waterQuantityMl = 120.0,
            onBrewingTimeChanged = {},
            onTemperatureChanged = {},
            onWaterQuantityChanged = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
