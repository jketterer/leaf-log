package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.analytics.hexToColor
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.screens.home.QuickBrewCardData
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun QuickBrewCard(
    data: QuickBrewCardData,
    userPreferences: UserPreferences,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 160.dp,
) {
    val accentColor = data.teaTypeColorHex?.hexToColor()
        ?: MaterialTheme.colorScheme.primaryContainer

    Card(
        onClick = onClick,
        modifier = modifier.width(cardWidth),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Vertical tea type accent bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(accentColor),
            )

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Config label
                Text(
                    text = data.configuration.label ?: "Unnamed Method",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Tea name
                Text(
                    text = data.teaName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Vessel name
                Text(
                    text = data.vesselName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Brewing params
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BrewingParamChip(
                        icon = FeatherIcons.Clock,
                        text = formatBrewingTime(data.configuration.brewingTime.inWholeSeconds.toInt()),
                    )
                    BrewingParamChip(
                        icon = FeatherIcons.Thermometer,
                        text = TemperatureFormatter.format(
                            data.configuration.temperatureCelsius,
                            userPreferences.temperatureUnit,
                        ),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickBrewCardPreview() {
    LeafLogTheme {
        QuickBrewCard(
            data = QuickBrewCardData(
                configuration = BrewingConfiguration(
                    id = "1",
                    teaId = "tea1",
                    vesselId = "vessel1",
                    teaQuantityGrams = 5f,
                    waterQuantityMl = 100.0,
                    temperatureCelsius = 90.0,
                    brewingTime = 30.seconds,
                    waterType = WaterType.FILTERED,
                    sourceSessionId = null,
                    rating = null,
                    label = "Gaiwan · Gong-fu",
                    isActive = true,
                    timesUsed = 12,
                    lastUsedAt = null,
                    createdAt = Instant.fromEpochMilliseconds(0),
                    updatedAt = Instant.fromEpochMilliseconds(0),
                ),
                teaName = "Tie Guan Yin",
                teaTypeName = "Oolong",
                teaTypeColorHex = "#FF9800",
                vesselName = "Gaiwan",
            ),
            userPreferences = UserPreferences(temperatureUnit = TemperatureUnit.CELSIUS),
            onClick = {},
        )
    }
}
