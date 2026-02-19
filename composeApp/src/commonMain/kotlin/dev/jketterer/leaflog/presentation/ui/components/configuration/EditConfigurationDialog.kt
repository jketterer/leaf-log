package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import kotlin.math.roundToInt
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
        waterQuantityMl: Double,
        temperatureCelsius: Double,
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

    // Track the source-of-truth value and the unit it was entered in.
    // When toggling units, we always convert from this original value to avoid
    // chained rounding errors (e.g. 175°F → 79°C → 174°F).
    var temperatureSourceValue by remember {
        mutableStateOf(
            temperatureUnit.fromCelsius(configuration.temperatureCelsius).toDouble()
        )
    }
    var temperatureSourceUnit by remember { mutableStateOf(temperatureUnit) }
    var temperature by remember {
        mutableStateOf(temperatureSourceValue.roundToInt().toString())
    }

    var waterQuantitySourceValue by remember {
        mutableStateOf(
            volumeUnit.fromMilliliters(configuration.waterQuantityMl).toDouble()
        )
    }
    var waterQuantitySourceUnit by remember { mutableStateOf(volumeUnit) }
    var waterQuantity by remember {
        mutableStateOf(waterQuantitySourceValue.roundToInt().toString())
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
                    onValueChange = { newValue ->
                        waterQuantity = newValue
                        // User typed a new value — this becomes the new source of truth
                        newValue.toDoubleOrNull()?.let {
                            waterQuantitySourceValue = it
                            waterQuantitySourceUnit = volumeUnit
                        }
                    },
                    currentUnit = volumeUnit,
                    onToggleUnit = {
                        val newUnit = when (volumeUnit) {
                            VolumeUnit.MILLILITERS -> VolumeUnit.FLUID_OUNCES
                            VolumeUnit.FLUID_OUNCES -> VolumeUnit.MILLILITERS
                        }
                        // Convert from original source value to new unit via floating point
                        val sourceMl = when (waterQuantitySourceUnit) {
                            VolumeUnit.MILLILITERS -> waterQuantitySourceValue
                            VolumeUnit.FLUID_OUNCES -> waterQuantitySourceValue * 29.5735
                        }
                        val converted = when (newUnit) {
                            VolumeUnit.MILLILITERS -> sourceMl
                            VolumeUnit.FLUID_OUNCES -> sourceMl / 29.5735
                        }
                        waterQuantity = converted.roundToInt().toString()
                        volumeUnit = newUnit
                    },
                    label = { Text("Water Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                )

                TemperatureInputField(
                    value = temperature,
                    onValueChange = { newValue ->
                        temperature = newValue
                        // User typed a new value — this becomes the new source of truth
                        newValue.toDoubleOrNull()?.let {
                            temperatureSourceValue = it
                            temperatureSourceUnit = temperatureUnit
                        }
                    },
                    currentUnit = temperatureUnit,
                    onToggleUnit = {
                        val newUnit = when (temperatureUnit) {
                            TemperatureUnit.CELSIUS -> TemperatureUnit.FAHRENHEIT
                            TemperatureUnit.FAHRENHEIT -> TemperatureUnit.CELSIUS
                        }
                        // Convert from original source value to new unit via floating point
                        val sourceCelsius = when (temperatureSourceUnit) {
                            TemperatureUnit.CELSIUS -> temperatureSourceValue
                            TemperatureUnit.FAHRENHEIT -> (temperatureSourceValue - 32) * 5.0 / 9.0
                        }
                        val converted = when (newUnit) {
                            TemperatureUnit.CELSIUS -> sourceCelsius
                            TemperatureUnit.FAHRENHEIT -> sourceCelsius * 9.0 / 5.0 + 32
                        }
                        temperature = converted.roundToInt().toString()
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
