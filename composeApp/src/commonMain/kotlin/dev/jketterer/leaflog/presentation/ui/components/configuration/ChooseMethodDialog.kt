package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Dialog for choosing between multiple brewing configurations
 */
@Composable
fun ChooseMethodDialog(
    configurations: List<BrewingConfiguration>,
    selectedConfigurationId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedId by remember { mutableStateOf(selectedConfigurationId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Brewing Method",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                configurations.forEach { config ->
                    ConfigurationOption(
                        configuration = config,
                        isSelected = selectedId == config.id,
                        isRecommended = config.id == configurations.firstOrNull()?.id,
                        onClick = { selectedId = if (selectedId == config.id) null else config.id }
                    )
                }

            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelect(selectedId)
                    onDismiss()
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfigurationOption(
    configuration: BrewingConfiguration,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick
            ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = configuration.label ?: "Unnamed Method",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (isRecommended) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "★ ${configuration.rating} · Used ${configuration.timesUsed} time(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChooseMethodDialogPreview() {
    val now = Instant.fromEpochSeconds(0)
    val configs = listOf(
        BrewingConfiguration(
            id = "1",
            teaId = "t1",
            vesselId = "v1",
            teaQuantityGrams = 5f,
            waterQuantityMl = 200.0,
            temperatureCelsius = 95.0,
            brewingTime = 3.minutes,
            waterType = WaterType.FILTERED,
            sourceSessionId = null,
            rating = 4.8f,
            timesUsed = 12,
            lastUsedAt = now,
            label = "Quick Brew",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        ),
        BrewingConfiguration(
            id = "2",
            teaId = "t1",
            vesselId = "v1",
            teaQuantityGrams = 4f,
            waterQuantityMl = 180.0,
            temperatureCelsius = 85.0,
            brewingTime = 4.minutes,
            waterType = WaterType.SPRING,
            sourceSessionId = null,
            rating = 3.5f,
            timesUsed = 0,
            lastUsedAt = null,
            label = "English Breakfast · Western",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        ),
    )
    LeafLogTheme {
        ChooseMethodDialog(
            configurations = configs,
            selectedConfigurationId = "1",
            onSelect = {},
            onDismiss = {},
        )
    }
}
