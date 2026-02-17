package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.TemperatureInputField
import dev.jketterer.leaflog.presentation.ui.components.common.VolumeInputField
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import kotlin.time.Duration

/**
 * Dialog for editing a brewing configuration
 */
@Composable
fun EditConfigurationDialog(
    configuration: BrewingConfiguration,
    userPreferences: UserPreferences,
    onSave: (
        label: String,
        teaQuantityGrams: Float?,
        waterQuantityMl: Int,
        temperatureCelsius: Int,
        brewingTime: Duration,
        waterType: WaterType,
        isActive: Boolean
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(configuration.label ?: "") }
    var teaQuantity by remember { mutableStateOf(configuration.teaQuantityGrams?.toString() ?: "") }
    var temperatureUnit by remember { mutableStateOf(userPreferences.temperatureUnit) }
    var volumeUnit by remember { mutableStateOf(userPreferences.volumeUnit) }
    var waterQuantity by remember {
        mutableStateOf(
            volumeUnit.fromMilliliters(configuration.waterQuantityMl).toString()
        )
    }
    var temperature by remember {
        mutableStateOf(
            temperatureUnit.fromCelsius(configuration.temperatureCelsius).toString()
        )
    }
    var brewingTime by remember { mutableStateOf(configuration.brewingTime) }
    var waterType by remember { mutableStateOf(configuration.waterType) }
    var isActive by remember { mutableStateOf(configuration.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Configuration",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = teaQuantity,
                    onValueChange = { teaQuantity = it },
                    label = { Text("Tea Quantity (g)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                VolumeInputField(
                    value = waterQuantity,
                    onValueChange = { waterQuantity = it },
                    currentUnit = volumeUnit,
                    onToggleUnit = {
                        val currentValue = waterQuantity.toDoubleOrNull()
                        val newUnit = when (volumeUnit) {
                            VolumeUnit.MILLILITERS -> VolumeUnit.FLUID_OUNCES
                            VolumeUnit.FLUID_OUNCES -> VolumeUnit.MILLILITERS
                        }
                        if (currentValue != null) {
                            // Direct conversion via floating point, rounding only once
                            val converted = when {
                                volumeUnit == VolumeUnit.MILLILITERS && newUnit == VolumeUnit.FLUID_OUNCES ->
                                    (currentValue / 29.5735).roundToInt()
                                volumeUnit == VolumeUnit.FLUID_OUNCES && newUnit == VolumeUnit.MILLILITERS ->
                                    (currentValue * 29.5735).roundToInt()
                                else -> currentValue.roundToInt()
                            }
                            waterQuantity = converted.toString()
                        }
                        volumeUnit = newUnit
                    },
                    label = { Text("Water Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                )

                TemperatureInputField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    currentUnit = temperatureUnit,
                    onToggleUnit = {
                        val currentValue = temperature.toDoubleOrNull()
                        val newUnit = when (temperatureUnit) {
                            TemperatureUnit.CELSIUS -> TemperatureUnit.FAHRENHEIT
                            TemperatureUnit.FAHRENHEIT -> TemperatureUnit.CELSIUS
                        }
                        if (currentValue != null) {
                            // Direct conversion via floating point, rounding only once
                            val converted = when {
                                temperatureUnit == TemperatureUnit.CELSIUS && newUnit == TemperatureUnit.FAHRENHEIT ->
                                    (currentValue * 9.0 / 5.0 + 32).roundToInt()
                                temperatureUnit == TemperatureUnit.FAHRENHEIT && newUnit == TemperatureUnit.CELSIUS ->
                                    ((currentValue - 32) * 5.0 / 9.0).roundToInt()
                                else -> currentValue.roundToInt()
                            }
                            temperature = converted.toString()
                        }
                        temperatureUnit = newUnit
                    },
                    label = { Text("Temperature") },
                    modifier = Modifier.fillMaxWidth(),
                )

                DurationPicker(
                    duration = brewingTime,
                    onDurationChange = { it?.let { brewingTime = it } },
                    label = "Brewing Time",
                    modifier = Modifier.fillMaxWidth()
                )

                WaterTypeSelector(
                    selectedWaterType = waterType,
                    onWaterTypeSelected = { waterType = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Text(
                        text = "Active (will be suggested when logging)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "Changes will apply to future sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        label,
                        teaQuantity.toFloatOrNull(),
                        waterQuantity.toIntOrNull()?.let { volumeUnit.toMilliliters(it) }
                            ?: configuration.waterQuantityMl,
                        temperature.toIntOrNull()?.let { temperatureUnit.toCelsius(it) }
                            ?: configuration.temperatureCelsius,
                        brewingTime,
                        waterType,
                        isActive
                    )
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
