package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Clock
import org.koin.compose.koinInject

/**
 * Dropdown selector for brewing vessel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesselSelector(
    vessels: List<BrewingVessel>,
    selectedVessel: BrewingVessel?,
    onVesselSelected: (BrewingVessel) -> Unit,
    label: String = "Brewing Vessel",
    volumeUnit: VolumeUnit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    imageStorage: ImageStorage = koinInject(),
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedVessel?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                isError = isError,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                        enabled = true,
                    ),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                vessels.forEach { vessel ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (vessel.imagePath != null) {
                                    AsyncImage(
                                        model = imageStorage.resolveImagePath(vessel.imagePath),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else if (vessel.iconName != null) {
                                    Text(
                                        text = getVesselEmoji(vessel.iconName),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(vessel.name)
                                    if (vessel.capacityMl != null) {
                                        Text(
                                            text = VolumeFormatter.format(
                                                milliliters = vessel.capacityMl.toDouble(),
                                                unit = volumeUnit,
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            onVesselSelected(vessel)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Map icon names to emojis.
 */
private fun getVesselEmoji(iconName: String): String {
    return when (iconName) {
        "gaiwan" -> "🫖"
        "teapot" -> "🫖"
        "kyusu" -> "🍵"
        "mug" -> "☕"
        "yixing" -> "🫖"
        "glass" -> "🫗"
        else -> "🍵"
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselSelectorPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VesselSelector(
                vessels = listOf(
                    BrewingVessel(
                        id = "gaiwan",
                        name = "Gaiwan",
                        iconName = "gaiwan",
                        isSystemDefault = true,
                        displayOrder = 0,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                    BrewingVessel(
                        id = "kyusu",
                        name = "Kyusu",
                        iconName = "kyusu",
                        isSystemDefault = true,
                        displayOrder = 1,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                    BrewingVessel(
                        id = "mug",
                        name = "Mug",
                        iconName = "mug",
                        isSystemDefault = true,
                        displayOrder = 2,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                ),
                selectedVessel = null,
                onVesselSelected = {},
                volumeUnit = VolumeUnit.FLUID_OUNCES,
            )
        }
    }
}