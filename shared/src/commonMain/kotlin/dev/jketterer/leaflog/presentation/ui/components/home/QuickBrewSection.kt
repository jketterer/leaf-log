package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.screens.home.QuickBrewCardData
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun QuickBrewSection(
    configurations: List<QuickBrewCardData>,
    userPreferences: UserPreferences,
    onConfigClick: (QuickBrewCardData) -> Unit,
    onEditClick: (QuickBrewCardData) -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "QUICK BREW",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onManageClick) {
                Text("Manage")
            }
        }

        val horizontalPadding = 16.dp
        val cardSpacing = 8.dp
        val visibleCards = 2.3f

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Calculate card width to show ~2.3 cards, clamped to [140dp, 200dp]
            val availableWidth = maxWidth - (horizontalPadding * 2)
            val totalSpacing = cardSpacing * (visibleCards - 1)
            val cardWidth = ((availableWidth - totalSpacing) / visibleCards)
                .coerceIn(140.dp, 200.dp)

            LazyRow(
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            ) {
                items(
                    items = configurations,
                    key = { it.configuration.id },
                ) { data ->
                    QuickBrewCard(
                        data = data,
                        userPreferences = userPreferences,
                        onClick = { onConfigClick(data) },
                        onLongClick = { onEditClick(data) },
                        cardWidth = cardWidth,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickBrewSectionPreview() {
    LeafLogTheme {
        QuickBrewSection(
            configurations = listOf(
                QuickBrewCardData(
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
                QuickBrewCardData(
                    configuration = BrewingConfiguration(
                        id = "2",
                        teaId = "tea2",
                        vesselId = "vessel2",
                        teaQuantityGrams = 2f,
                        waterQuantityMl = 350.0,
                        temperatureCelsius = 80.0,
                        brewingTime = 3.5.minutes,
                        waterType = WaterType.FILTERED,
                        sourceSessionId = null,
                        rating = null,
                        label = "Mug · Western",
                        isActive = true,
                        timesUsed = 5,
                        lastUsedAt = null,
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0),
                    ),
                    teaName = "Sencha",
                    teaTypeName = "Green",
                    teaTypeColorHex = "#4CAF50",
                    vesselName = "Mug",
                ),
            ),
            userPreferences = UserPreferences(temperatureUnit = TemperatureUnit.CELSIUS),
            onConfigClick = {},
            onEditClick = {},
            onManageClick = {},
        )
    }
}
