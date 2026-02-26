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
import compose.icons.FeatherIcons
import compose.icons.feathericons.PlusCircle
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
