package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.UserPreferences

/**
 * Section showing saved brewing methods for a tea
 * To be used in Tea Detail Screen
 */
@Composable
fun SavedMethodsSection(
    configurations: List<BrewingConfiguration>,
    getVesselName: (String) -> String,
    userPreferences: UserPreferences,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (configurations.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SAVED BREWING METHODS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        configurations.forEach { config ->
            ConfigurationCard(
                configuration = config,
                vesselName = getVesselName(config.vesselId),
                userPreferences = userPreferences,
                onEdit = { onEdit(config.id) },
                onDelete = { onDelete(config.id) }
            )
        }
    }
}
