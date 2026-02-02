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
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import kotlin.time.Duration

/**
 * Dialog for editing a brewing configuration
 */
@Composable
fun EditConfigurationDialog(
    configuration: BrewingConfiguration,
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
    var waterQuantity by remember { mutableStateOf(configuration.waterQuantityMl.toString()) }
    var temperature by remember { mutableStateOf(configuration.temperatureCelsius.toString()) }
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

                OutlinedTextField(
                    value = waterQuantity,
                    onValueChange = { waterQuantity = it },
                    label = { Text("Water Quantity (ml)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperature (°C)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("°C") }
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
                        waterQuantity.toIntOrNull() ?: configuration.waterQuantityMl,
                        temperature.toIntOrNull() ?: configuration.temperatureCelsius,
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
