package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Thermometer
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.timeAgo
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.last_used
import leaf_log.shared.generated.resources.times_used
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Card displaying a saved brewing configuration
 */
@Composable
fun ConfigurationCard(
    configuration: BrewingConfiguration,
    vesselName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header row with label and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = configuration.label ?: "Unnamed Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (configuration.rating != null) {
                        RatingDisplay(configuration.rating, iconsOnly = true)
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = FeatherIcons.Edit2,
                            contentDescription = "Edit",
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = FeatherIcons.Trash2,
                            contentDescription = "Delete",
                        )
                    }
                }
            }

            // Vessel name
            Text(
                text = "$vesselName • ${
                    formatVolume(configuration.waterQuantityMl)
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Stats row
            val stats = buildList {
                val timesUsed = configuration.timesUsed
                add(pluralStringResource(Res.plurals.times_used, timesUsed, timesUsed))
                configuration.lastUsedAt?.let { lastUsed ->
                    add(stringResource(Res.string.last_used, timeAgo(lastUsed)))
                }
            }

            Text(
                text = stats.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            // Parameters as chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                configuration.teaQuantityGrams?.let {
                    BrewingParamChip(
                        vectorResource(Res.drawable.ic_tea_leaf),
                        formatWeight(it)
                    )
                }
                BrewingParamChip(
                    FeatherIcons.Thermometer,
                    formatTemperature(configuration.temperatureCelsius)
                )
                BrewingParamChip(
                    FeatherIcons.Clock,
                    formatBrewingTime(configuration.brewingTime.inWholeSeconds.toInt())
                )
                BrewingParamChip(
                    FeatherIcons.Droplet,
                    formatVolume(configuration.waterQuantityMl)
                )
            }
        }
    }
}


@Preview
@Composable
private fun ConfigurationCardPreview() {
    LeafLogTheme {
        ConfigurationCard(
            configuration = BrewingConfiguration(
                id = "1",
                teaId = "tea-1",
                vesselId = "vessel-1",
                teaQuantityGrams = 5.0f,
                waterQuantityMl = 200.0,
                temperatureCelsius = 95.0,
                brewingTime = 3.minutes,
                waterType = WaterType.FILTERED,
                sourceSessionId = "session-1",
                rating = 4.5f,
                timesUsed = 12,
                lastUsedAt = PreviewInstant,
                label = "Morning Gongfu",
                createdAt = PreviewInstant,
                updatedAt = PreviewInstant
            ),
            vesselName = "Gaiwan (100ml)",
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview
@Composable
private fun ConfigurationCardImperialUnitsPreview() {
    LeafLogTheme(useDarkTheme = true) {
        ConfigurationCard(
            configuration = BrewingConfiguration(
                id = "3",
                teaId = "tea-3",
                vesselId = "vessel-3",
                teaQuantityGrams = 7.5f,
                waterQuantityMl = 355.0,
                temperatureCelsius = 100.0,
                brewingTime = 2.minutes + 30.seconds,
                waterType = WaterType.FILTERED,
                sourceSessionId = "session-3",
                rating = 5.0f,
                timesUsed = 25,
                lastUsedAt = PreviewInstant - 800.days,
                label = "Perfect Black Tea",
                createdAt = PreviewInstant,
                updatedAt = PreviewInstant
            ),
            vesselName = "Ceramic Mug",
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview
@Composable
private fun ConfigurationCardUnnamedPreview() {
    LeafLogTheme {
        ConfigurationCard(
            configuration = BrewingConfiguration(
                id = "4",
                teaId = "tea-4",
                vesselId = "vessel-4",
                teaQuantityGrams = 3.0f,
                waterQuantityMl = 150.0,
                temperatureCelsius = 80.0,
                brewingTime = 1.minutes,
                waterType = WaterType.TAP,
                sourceSessionId = "session-4",
                rating = null,
                timesUsed = 1,
                lastUsedAt = PreviewInstant - 365.minutes,
                label = null,
                createdAt = PreviewInstant,
                updatedAt = PreviewInstant
            ),
            vesselName = "Small Cup",
            onEdit = {},
            onDelete = {}
        )
    }
}
