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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSessionParametersSheet(
    temperatureValue: String,
    waterQuantityValue: String,
    teaQuantityValue: String,
    isTeaBag: Boolean,
    waterType: WaterType,
    temperatureUnit: TemperatureUnit,
    volumeUnit: VolumeUnit,
    onTemperatureChanged: (String) -> Unit,
    onWaterQuantityChanged: (String) -> Unit,
    onTeaQuantityChanged: (String) -> Unit,
    onTeaBagModeChanged: (Boolean) -> Unit,
    onWaterTypeSelected: (WaterType) -> Unit,
    onToggleTemperatureUnit: () -> Unit,
    onToggleVolumeUnit: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        EditSessionParametersContent(
            temperatureValue = temperatureValue,
            waterQuantityValue = waterQuantityValue,
            teaQuantityValue = teaQuantityValue,
            isTeaBag = isTeaBag,
            waterType = waterType,
            temperatureUnit = temperatureUnit,
            volumeUnit = volumeUnit,
            onTemperatureChanged = onTemperatureChanged,
            onWaterQuantityChanged = onWaterQuantityChanged,
            onTeaQuantityChanged = onTeaQuantityChanged,
            onTeaBagModeChanged = onTeaBagModeChanged,
            onWaterTypeSelected = onWaterTypeSelected,
            onToggleTemperatureUnit = onToggleTemperatureUnit,
            onToggleVolumeUnit = onToggleVolumeUnit,
            onSave = onSave,
            onDismiss = onDismiss,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSessionParametersContent(
    temperatureValue: String,
    waterQuantityValue: String,
    teaQuantityValue: String,
    isTeaBag: Boolean,
    waterType: WaterType,
    temperatureUnit: TemperatureUnit,
    volumeUnit: VolumeUnit,
    onTemperatureChanged: (String) -> Unit,
    onWaterQuantityChanged: (String) -> Unit,
    onTeaQuantityChanged: (String) -> Unit,
    onTeaBagModeChanged: (Boolean) -> Unit,
    onWaterTypeSelected: (WaterType) -> Unit,
    onToggleTemperatureUnit: () -> Unit,
    onToggleVolumeUnit: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isValid = temperatureValue.isNotBlank() &&
            waterQuantityValue.isNotBlank() &&
            (isTeaBag || teaQuantityValue.isNotBlank())
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Edit Parameters",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Temperature
        TemperatureInputField(
            value = temperatureValue,
            onValueChange = onTemperatureChanged,
            currentUnit = temperatureUnit,
            onToggleUnit = onToggleTemperatureUnit,
            label = { Text("Temperature") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Water Quantity
        VolumeInputField(
            value = waterQuantityValue,
            onValueChange = onWaterQuantityChanged,
            currentUnit = volumeUnit,
            onToggleUnit = onToggleVolumeUnit,
            label = { Text("Water Quantity") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tea type selector
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
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = teaQuantityValue,
                onValueChange = onTeaQuantityChanged,
                label = { Text("Tea Quantity") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                suffix = { Text("g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Water Type
        WaterTypeSelector(
            selectedWaterType = waterType,
            onWaterTypeSelected = onWaterTypeSelected,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onSave,
                enabled = isValid,
                modifier = Modifier.weight(1f),
            ) {
                Text("Save")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditSessionParametersContentLooseLeafPreview() {
    LeafLogTheme {
        EditSessionParametersContent(
            temperatureValue = "80",
            waterQuantityValue = "200",
            teaQuantityValue = "5",
            isTeaBag = false,
            waterType = WaterType.FILTERED,
            temperatureUnit = TemperatureUnit.CELSIUS,
            volumeUnit = VolumeUnit.MILLILITERS,
            onTemperatureChanged = {},
            onWaterQuantityChanged = {},
            onTeaQuantityChanged = {},
            onTeaBagModeChanged = {},
            onWaterTypeSelected = {},
            onToggleTemperatureUnit = {},
            onToggleVolumeUnit = {},
            onSave = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditSessionParametersContentTeaBagPreview() {
    LeafLogTheme {
        EditSessionParametersContent(
            temperatureValue = "80",
            waterQuantityValue = "200",
            teaQuantityValue = "",
            isTeaBag = true,
            waterType = WaterType.FILTERED,
            temperatureUnit = TemperatureUnit.CELSIUS,
            volumeUnit = VolumeUnit.MILLILITERS,
            onTemperatureChanged = {},
            onWaterQuantityChanged = {},
            onTeaQuantityChanged = {},
            onTeaBagModeChanged = {},
            onWaterTypeSelected = {},
            onToggleTemperatureUnit = {},
            onToggleVolumeUnit = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
