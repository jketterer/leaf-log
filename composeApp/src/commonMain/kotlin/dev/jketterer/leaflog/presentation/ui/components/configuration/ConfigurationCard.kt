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
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaflog.composeapp.generated.resources.Res
import leaflog.composeapp.generated.resources.ic_tea_leaf
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Clock
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
    userPreferences: UserPreferences,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.isActive) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
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
                    VolumeFormatter.format(
                        configuration.waterQuantityMl,
                        userPreferences.volumeUnit
                    )
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Stats row
            val stats = buildList {
                add("Used ${configuration.timesUsed} times")
                configuration.lastUsedAt?.let { lastUsed ->
                    val daysAgo = (Clock.System.now() - lastUsed).inWholeDays
                    val timeAgo = when {
                        daysAgo == 0L -> "today"
                        daysAgo == 1L -> "yesterday"
                        daysAgo < 7 -> "$daysAgo days ago"
                        daysAgo < 30 -> "${daysAgo / 7} week(s) ago"
                        daysAgo < 365 -> "${daysAgo / 30} month(s) ago"
                        else -> "${daysAgo / 365} year(s) ago"
                    }
                    add("Last: $timeAgo")
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
                        "${it}g"
                    )
                }
                BrewingParamChip(
                    FeatherIcons.Thermometer,
                    TemperatureFormatter.format(
                        configuration.temperatureCelsius,
                        userPreferences.temperatureUnit
                    )
                )
                BrewingParamChip(
                    FeatherIcons.Clock,
                    formatBrewingTime(configuration.brewingTime.inWholeSeconds.toInt())
                )
                BrewingParamChip(
                    FeatherIcons.Droplet,
                    VolumeFormatter.format(
                        configuration.waterQuantityMl,
                        userPreferences.volumeUnit
                    )
                )
            }
        }
    }
}


@Preview
@Composable
private fun ConfigurationCardActivePreview() {
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
                lastUsedAt = Clock.System.now(),
                label = "Morning Gongfu",
                isActive = true,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            vesselName = "Gaiwan (100ml)",
            userPreferences = UserPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                volumeUnit = VolumeUnit.MILLILITERS
            ),
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview
@Composable
private fun ConfigurationCardInactivePreview() {
    LeafLogTheme {
        ConfigurationCard(
            configuration = BrewingConfiguration(
                id = "2",
                teaId = "tea-2",
                vesselId = "vessel-2",
                teaQuantityGrams = null,
                waterQuantityMl = 355.0,
                temperatureCelsius = 85.0,
                brewingTime = 45.seconds,
                waterType = WaterType.SPRING,
                sourceSessionId = "session-2",
                rating = 3.0f,
                timesUsed = 3,
                lastUsedAt = null,
                label = "Quick Steep",
                isActive = false,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            vesselName = "Teapot",
            userPreferences = UserPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                volumeUnit = VolumeUnit.MILLILITERS
            ),
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
                lastUsedAt = Clock.System.now() - 800.days,
                label = "Perfect Black Tea",
                isActive = true,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            vesselName = "Ceramic Mug",
            userPreferences = UserPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                volumeUnit = VolumeUnit.FLUID_OUNCES
            ),
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
                lastUsedAt = Clock.System.now() - 365.minutes,
                label = null,
                isActive = true,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            vesselName = "Small Cup",
            userPreferences = UserPreferences(),
            onEdit = {},
            onDelete = {}
        )
    }
}
