package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Thermometer
import compose.icons.feathericons.TrendingUp
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.formatDate
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatSteepIncrement
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.common.timeAgo
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.edit
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.method_not_brewed
import leaf_log.shared.generated.resources.method_source
import leaf_log.shared.generated.resources.method_usage
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.unnamed_method
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Card displaying a saved brewing configuration
 *
 * @param source The session the method was saved from, null when there was none or it's gone
 */
@Composable
fun ConfigurationCard(
    configuration: BrewingConfiguration,
    vesselName: String,
    source: SessionWithSteeps?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSourceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = configuration.label ?: stringResource(Res.string.unnamed_method),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    // The method keeps a copy of its brew's rating from when it was saved
                    val rating = if (source != null) source.effectiveRating else configuration.rating
                    rating?.let { RatingDisplay(it) }
                    Text(
                        text = configuration.lastUsedAt?.let { lastUsed ->
                            val timesUsed = configuration.timesUsed
                            pluralStringResource(Res.plurals.method_usage, timesUsed, timesUsed, timeAgo(lastUsed))
                        } ?: stringResource(Res.string.method_not_brewed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = FeatherIcons.MoreVertical,
                            contentDescription = stringResource(Res.string.more_options),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            Text(
                text = vesselName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                configuration.teaQuantityGrams?.let {
                    BrewingParamChip(vectorResource(Res.drawable.ic_tea_leaf), formatWeight(it))
                }
                BrewingParamChip(FeatherIcons.Thermometer, formatTemperature(configuration.temperatureCelsius))
                BrewingParamChip(FeatherIcons.Clock, formatBrewingTime(configuration.brewingTime.inWholeSeconds.toInt()))
                configuration.steepIncrement?.let { step ->
                    BrewingParamChip(FeatherIcons.TrendingUp, formatSteepIncrement(step))
                }
                BrewingParamChip(FeatherIcons.Droplet, formatVolume(configuration.waterQuantityMl))
            }

            source?.let { session ->
                val date = session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                Row(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button, onClick = onSourceClick),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.method_source, TimeFormatter.formatDate(date, currentDate())),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = FeatherIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
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
                waterQuantityMl = 100.0,
                temperatureCelsius = 95.0,
                brewingTime = 20.seconds,
                steepIncrement = 10.seconds,
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
            source = previewSession(ratings = listOf(5f, 4f), status = SessionStatus.COMPLETED),
            onEdit = {},
            onDelete = {},
            onSourceClick = {},
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
            source = null,
            onEdit = {},
            onDelete = {},
            onSourceClick = {},
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
            source = null,
            onEdit = {},
            onDelete = {},
            onSourceClick = {},
        )
    }
}
