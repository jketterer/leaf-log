package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.TemperatureInputField
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

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
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(
                        listOf("+5s" to 5.seconds, "+10s" to 10.seconds, "+30s" to 30.seconds, "+1m" to 1.minutes),
                        listOf("-5s" to 5.seconds, "-10s" to 10.seconds, "-30s" to 30.seconds, "-1m" to 1.minutes),
                    ).forEachIndexed { rowIndex, adjustments ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            adjustments.forEach { (label, amount) ->
                                OutlinedButton(
                                    onClick = {
                                        val current = duration ?: Duration.ZERO
                                        val next = if (rowIndex == 0) current + amount else current - amount
                                        onDurationChange(maxOf(next, Duration.ZERO))
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        listOf("-5°" to -5, "+5°" to 5).forEach { (label, delta) ->
                            OutlinedButton(
                                onClick = {
                                    val currentDisplay = temperatureUnit.fromCelsius(temperature)
                                    val newCelsius = temperatureUnit.toCelsius(currentDisplay + delta)
                                    onTemperatureChange(newCelsius.coerceIn(0.0, 100.0))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
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

private val previewSession = TeaSession(
    id = "1",
    teaId = "tea-1",
    steepNumber = 1,
    vesselId = "vessel-1",
    waterType = WaterType.FILTERED,
    timestamp = Instant.fromEpochMilliseconds(1),
    status = SessionStatus.IN_PROGRESS,
    brewingTime = 60.seconds,
    updatedAt = Instant.fromEpochMilliseconds(1),
    deletedAt = null,
    createdAt = Instant.fromEpochMilliseconds(1),
    temperatureCelsius = 80.0,
    waterQuantityMl = 200.0,
    photos = emptyList(),
    syncStatus = SyncStatus.LOCAL_ONLY,
)

@Preview
@Composable
private fun NextSteepParameterDialogCelsiusPreview() {
    LeafLogTheme {
        NextSteepParameterDialog(
            currentSession = previewSession,
            duration = 60.seconds,
            temperature = 80.0,
            temperatureUnit = TemperatureUnit.CELSIUS,
            onDurationChange = {},
            onTemperatureChange = {},
            onToggleUnit = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun NextSteepParameterDialogFahrenheitPreview() {
    LeafLogTheme {
        NextSteepParameterDialog(
            currentSession = previewSession.copy(steepNumber = 2),
            duration = 1.minutes + 30.seconds,
            temperature = 95.0,
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            onDurationChange = {},
            onTemperatureChange = {},
            onToggleUnit = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}