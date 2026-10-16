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
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.presentation.ui.components.common.DialogPreviewContainer
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.UnitInputField
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_temperature
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun NextSteepParameterDialog(
    nextSteepNumber: Int,
    duration: Duration?,
    temperatureCelsius: Double?,
    temperatureUnit: TemperatureUnit,
    onDurationChange: (Duration?) -> Unit,
    onTemperatureChange: (Double?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isValid = duration != null && duration > Duration.ZERO &&
            temperatureCelsius != null && temperatureCelsius > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Next Steep Parameters")
        },
        text = {
            NextSteepParameterDialogContent(
                nextSteepNumber = nextSteepNumber,
                duration = duration,
                temperatureCelsius = temperatureCelsius,
                temperatureUnit = temperatureUnit,
                onDurationChange = onDurationChange,
                onTemperatureChange = onTemperatureChange,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isValid,
            ) {
                Text("Start Steep $nextSteepNumber")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun NextSteepParameterDialogContent(
    nextSteepNumber: Int,
    duration: Duration?,
    temperatureCelsius: Double?,
    temperatureUnit: TemperatureUnit,
    onDurationChange: (Duration?) -> Unit,
    onTemperatureChange: (Double?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Adjust brewing parameters for Steep $nextSteepNumber",
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
            UnitInputField(
                value = temperatureCelsius,
                onValueChange = onTemperatureChange,
                unit = temperatureUnit,
                label = stringResource(Res.string.field_temperature),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Steps in the Settings unit, even while the field shows the other one
                listOf("-5°" to -5, "+5°" to 5).forEach { (label, delta) ->
                    OutlinedButton(
                        onClick = {
                            val celsius = temperatureCelsius ?: return@OutlinedButton
                            val stepped = temperatureUnit.toCelsius(temperatureUnit.fromCelsius(celsius) + delta)
                            onTemperatureChange(stepped.coerceIn(0.0, 100.0))
                        },
                        enabled = temperatureCelsius != null,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NextSteepParameterDialogCelsiusPreview() {
    LeafLogTheme {
        DialogPreviewContainer(title = "Next Steep Parameters") {
            NextSteepParameterDialogContent(
                nextSteepNumber = 2,
                duration = 60.seconds,
                temperatureCelsius = 80.0,
                temperatureUnit = TemperatureUnit.CELSIUS,
                onDurationChange = {},
                onTemperatureChange = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NextSteepParameterDialogFahrenheitPreview() {
    LeafLogTheme {
        DialogPreviewContainer(title = "Next Steep Parameters") {
            NextSteepParameterDialogContent(
                nextSteepNumber = 3,
                duration = 1.minutes + 30.seconds,
                temperatureCelsius = 95.0,
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                onDurationChange = {},
                onTemperatureChange = {},
            )
        }
    }
}
