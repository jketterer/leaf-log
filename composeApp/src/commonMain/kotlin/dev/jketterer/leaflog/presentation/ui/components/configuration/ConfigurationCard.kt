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
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with label and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = configuration.label ?: "Unnamed Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = FeatherIcons.Edit2,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = FeatherIcons.Trash2,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Vessel name
            Text(
                text = vesselName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Parameters
            val params = buildList {
                configuration.teaQuantityGrams?.let { add("${it}g") }
                add("${configuration.waterQuantityMl}ml")
                add("${configuration.temperatureCelsius}°C")
                add(formatBrewingTime(configuration.brewingTime.inWholeSeconds.toInt()))
            }
            Text(
                text = params.joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium
            )

            // Stats row
            val stats = buildList {
                add("${configuration.rating}⭐")
                add("Used ${configuration.timesUsed} times")
                configuration.lastUsedAt?.let { lastUsed ->
                    val daysAgo = (Clock.System.now() - lastUsed).inWholeDays
                    val timeAgo = when {
                        daysAgo == 0L -> "today"
                        daysAgo == 1L -> "yesterday"
                        daysAgo < 7 -> "$daysAgo days ago"
                        daysAgo < 30 -> "${daysAgo / 7} weeks ago"
                        else -> "${daysAgo / 30} months ago"
                    }
                    add("Last: $timeAgo")
                }
            }

            Text(
                text = stats.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatBrewingTime(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds % 60 == 0 -> "${seconds / 60}m"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
}
