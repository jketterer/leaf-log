package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.TemperatureInputField
import kotlin.time.Duration

@Composable
fun NextSteepParameterDialog(
    currentSession: TeaSession,
    duration: Duration?,
    temperature: Double,
    temperatureUnit: TemperatureUnit,
    onDurationChange: (Duration?) -> Unit,
    onTemperatureChange: (Double) -> Unit,
    onToggleUnit: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Check if all fields are valid
    val isValid =
        duration != null && duration > Duration.ZERO && temperature > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Next Steep Parameters")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Adjust brewing parameters for Steep ${currentSession.steepNumber + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Duration
                Column {
                    Text(
                        text = "Brewing Time",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DurationPicker(
                        duration = duration,
                        onDurationChange = onDurationChange,
                        isError = duration == null || duration <= Duration.ZERO,
                        errorMessage = when {
                            duration == null -> "Please enter a valid brewing time"
                            duration <= Duration.ZERO -> "Brewing time must be greater than 0"
                            else -> null
                        },
                    )
                }

                // Temperature
                Column {
                    Text(
                        text = "Water Temperature",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Convert from storage (Celsius Double) to display unit (Int) for showing
                    val displayValue = temperatureUnit.fromCelsius(temperature).toString()

                    TemperatureInputField(
                        value = displayValue,
                        onValueChange = { value ->
                            // Convert from display unit (Int) back to storage (Celsius Double)
                            value.toIntOrNull()?.let { displayTemp ->
                                val celsiusTemp = temperatureUnit.toCelsius(displayTemp)
                                onTemperatureChange(celsiusTemp)
                            }
                        },
                        currentUnit = temperatureUnit,
                        onToggleUnit = onToggleUnit,
                        label = { Text("Temperature") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isValid,
            ) {
                Text("Start Steep ${currentSession.steepNumber + 1}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// TODO: add preview(s)