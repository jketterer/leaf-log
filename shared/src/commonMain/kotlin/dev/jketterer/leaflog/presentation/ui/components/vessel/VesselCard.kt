package dev.jketterer.leaflog.presentation.ui.components.vessel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Clock
import org.koin.compose.koinInject

@Composable
fun VesselCard(
    vessel: BrewingVessel,
    volumeUnit: VolumeUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageStorage: ImageStorage = koinInject(),
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (vessel.isArchived) 0.6f else 1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Vessel image or icon
                if (vessel.imagePath != null) {
                    AsyncImage(
                        model = imageStorage.resolveImagePath(vessel.imagePath),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        painter = VesselIconHelper.getIconForVessel(vessel.iconName),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Vessel name and capacity
                Column {
                    Text(
                        text = vessel.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    // Show capacity if set
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

                    if (vessel.isArchived) {
                        Text(
                            text = "Archived",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Chevron
            Icon(
                imageVector = FeatherIcons.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselCardPreview() {
    val now = Clock.System.now()
    LeafLogTheme {
        VesselCard(
            vessel = BrewingVessel(
                id = "1",
                name = "Gaiwan",
                iconName = "gaiwan",
                capacityMl = 100,
                isSystemDefault = true,
                displayOrder = 0,
                createdAt = now,
                updatedAt = now,
            ),
            volumeUnit = VolumeUnit.MILLILITERS,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselCardArchivedPreview() {
    val now = Clock.System.now()
    LeafLogTheme {
        VesselCard(
            vessel = BrewingVessel(
                id = "3",
                name = "Old Gaiwan",
                iconName = "gaiwan",
                capacityMl = 60,
                isSystemDefault = false,
                isArchived = true,
                displayOrder = 2,
                createdAt = now,
                updatedAt = now,
            ),
            volumeUnit = VolumeUnit.MILLILITERS,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselCardNoCapacityPreview() {
    val now = Clock.System.now()
    LeafLogTheme {
        VesselCard(
            vessel = BrewingVessel(
                id = "2",
                name = "Travel Mug",
                iconName = "mug",
                capacityMl = null,
                isSystemDefault = false,
                displayOrder = 1,
                createdAt = now,
                updatedAt = now,
            ),
            volumeUnit = VolumeUnit.FLUID_OUNCES,
            onClick = {},
        )
    }
}
