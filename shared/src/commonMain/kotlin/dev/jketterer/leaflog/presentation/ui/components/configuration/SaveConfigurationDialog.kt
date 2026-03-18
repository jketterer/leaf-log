package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bookmark
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Dialog to save a brewing configuration after a great session
 */
@OptIn(ExperimentalLayoutApi::class)
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
    userPreferences: UserPreferences,
    onSave: (label: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(suggestedLabel) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FeatherIcons.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Save as Brewing Method",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Tea + Vessel + Rating
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$teaName in $vesselName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (rating > 0f) {
                        RatingDisplay(rating = rating, iconsOnly = true)
                    }
                }

                // Parameters as chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (teaQuantityGrams != null) {
                        BrewingParamChip(icon = FeatherIcons.Coffee, text = "${teaQuantityGrams}g")
                    } else {
                        BrewingParamChip(icon = FeatherIcons.Coffee, text = "Tea Bag")
                    }
                    BrewingParamChip(icon = FeatherIcons.Droplet, text = VolumeFormatter.format(waterQuantityMl, userPreferences.volumeUnit))
                    BrewingParamChip(icon = FeatherIcons.Thermometer, text = TemperatureFormatter.format(temperatureCelsius, userPreferences.temperatureUnit))
                    BrewingParamChip(icon = FeatherIcons.Clock, text = formatBrewingTime(brewingTimeSeconds))
                }

                // Editable label
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name this method") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )

                // Help text
                Text(
                    text = "This will be suggested next time you brew this combination",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(label.ifBlank { suggestedLabel }) }
            ) {
                Text("Save Method")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        },
    )
}

private fun formatBrewingTime(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds % 60 == 0 -> "${seconds / 60}m"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
}

@Preview
@Composable
private fun SaveConfigurationDialogPreview() {
    LeafLogTheme {
        SaveConfigurationDialog(
            teaName = "Dragon Well",
            vesselName = "Gaiwan (100ml)",
            teaQuantityGrams = 5.0f,
            waterQuantityMl = 200.0,
            temperatureCelsius = 85.0,
            brewingTimeSeconds = 45,
            rating = 4.5f,
            suggestedLabel = "Gongfu Style",
            userPreferences = UserPreferences(),
            onSave = {},
            onDismiss = {},
        )
    }
}
