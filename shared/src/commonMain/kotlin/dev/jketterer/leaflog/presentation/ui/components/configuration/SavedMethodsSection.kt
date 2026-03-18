package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import compose.icons.FeatherIcons
import compose.icons.feathericons.PlusCircle
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Section showing saved brewing methods for a tea
 * To be used in Tea Detail Screen
 */
@Composable
fun SavedMethodsSection(
    configurations: List<BrewingConfiguration>,
    getVesselName: (String) -> String,
    userPreferences: UserPreferences,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SAVED BREWING METHODS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = FeatherIcons.PlusCircle,
                    contentDescription = "Add brewing method",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (configurations.isEmpty()) {
            Text(
                text = "No saved methods yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            configurations.forEach { config ->
                ConfigurationCard(
                    configuration = config,
                    vesselName = getVesselName(config.vesselId),
                    userPreferences = userPreferences,
                    onEdit = { onEdit(config.id) },
                    onDelete = { onDelete(config.id) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedMethodsSectionEmptyPreview() {
    LeafLogTheme {
        SavedMethodsSection(
            configurations = emptyList(),
            getVesselName = { "Gaiwan" },
            userPreferences = UserPreferences(),
            onAdd = {},
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedMethodsSectionWithItemsPreview() {
    val now = Clock.System.now()
    LeafLogTheme {
        SavedMethodsSection(
            configurations = listOf(
                BrewingConfiguration(
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
                    lastUsedAt = now,
                    label = "Morning Gongfu",
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
            getVesselName = { "Gaiwan (100ml)" },
            userPreferences = UserPreferences(),
            onAdd = {},
            onEdit = {},
            onDelete = {},
        )
    }
}
