package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dialog to save a brewing configuration after a great session
 */
@Composable
fun SaveConfigurationDialog(
    teaName: String,
    vesselName: String,
    teaQuantityGrams: Float?,
    waterQuantityMl: Double,
    temperatureCelsius: Double,
    brewingTimeSeconds: Int,
    rating: Float,
    suggestedLabel: String,
    onSave: (label: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(suggestedLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "💡 Save This as a Brewing Method?",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tea + Vessel info
                Text(
                    text = "$teaName in $vesselName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Parameters summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val params = buildList {
                        teaQuantityGrams?.let { add("${it}g") }
                        add("${waterQuantityMl.toInt()}ml")
                        add("${temperatureCelsius.toInt()}°C")
                        add(formatBrewingTime(brewingTimeSeconds))
                    }
                    Text(
                        text = params.joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Editable label
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name this method") },
                    placeholder = { Text(suggestedLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Help text
                Text(
                    text = "This will be suggested next time you brew this combination",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label.ifBlank { suggestedLabel }) }
            ) {
                Text("Save Method")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}

private fun formatBrewingTime(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds % 60 == 0 -> "${seconds / 60}m"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
}
