package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import compose.icons.FeatherIcons
import compose.icons.feathericons.PlusCircle
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration.Companion.minutes
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.add_method
import leaf_log.shared.generated.resources.header_saved_methods
import leaf_log.shared.generated.resources.no_saved_methods
import org.jetbrains.compose.resources.stringResource

/**
 * Section showing saved brewing methods for a tea
 * To be used in Tea Detail Screen
 */
@Composable
fun SavedMethodsSection(
    configurations: List<BrewingConfiguration>,
    getVesselName: (String) -> String,
    getSource: (BrewingConfiguration) -> SessionWithSteeps?,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(stringResource(Res.string.header_saved_methods)) {
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = FeatherIcons.PlusCircle,
                    contentDescription = stringResource(Res.string.add_method),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (configurations.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_saved_methods),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            configurations.forEach { config ->
                ConfigurationCard(
                    configuration = config,
                    vesselName = getVesselName(config.vesselId),
                    source = getSource(config),
                    onEdit = { onEdit(config.id) },
                    onDelete = { onDelete(config.id) },
                    onSourceClick = { config.sourceSessionId?.let(onSourceClick) },
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
            getSource = { null },
            onAdd = {},
            onEdit = {},
            onDelete = {},
            onSourceClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedMethodsSectionWithItemsPreview() {
    val now = PreviewInstant
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
                    createdAt = now,
                    updatedAt = now,
                ),
                BrewingConfiguration(
                    id = "2",
                    teaId = "tea-1",
                    vesselId = "vessel-1",
                    teaQuantityGrams = null,
                    waterQuantityMl = 300.0,
                    temperatureCelsius = 85.0,
                    brewingTime = 4.minutes,
                    waterType = WaterType.TAP,
                    sourceSessionId = null,
                    rating = null,
                    timesUsed = 0,
                    lastUsedAt = null,
                    label = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
            getVesselName = { "Gaiwan (100ml)" },
            getSource = { config ->
                config.sourceSessionId?.let {
                    previewSession(id = it, ratings = listOf(4.5f), status = SessionStatus.COMPLETED)
                }
            },
            onAdd = {},
            onEdit = {},
            onDelete = {},
            onSourceClick = {},
        )
    }
}
