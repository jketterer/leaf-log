package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.formatRelativeTimestamp
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.currentInstant
import dev.jketterer.leaflog.presentation.ui.components.common.formatDuration
import dev.jketterer.leaflog.presentation.ui.components.common.formatRating
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.brew_again
import leaf_log.shared.generated.resources.brewing_steep_n
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.rating_unrated
import leaf_log.shared.generated.resources.steep_count
import leaf_log.shared.generated.resources.steep_ratings
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * One session, for reading back: the tea and how it rated, when and how long, each steep's rating,
 * its dominant [tags] and note, then steep 1's parameters, quieter.
 */
@Composable
fun SessionCard(
    modifier: Modifier = Modifier,
    session: SessionWithSteeps,
    teaName: String,
    tags: List<TastingTag>,
    onSessionClick: () -> Unit,
    onBrewAgainClick: () -> Unit,
    onDeleteClick: () -> Unit,
    canBrewAgain: Boolean = true,
) {
    var showMenu by remember { mutableStateOf(false) }
    val first = session.steeps.first()
    val last = session.steeps.last()
    val inProgress = session.session.status == SessionStatus.IN_PROGRESS
    val steepCount = session.steeps.size

    Card(
        onClick = onSessionClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = teaName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (inProgress) {
                    Text(
                        text = stringResource(Res.string.brewing_steep_n, last.number),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    RatingDisplay(session.effectiveRating)
                }

                val metaStyle = MaterialTheme.typography.bodySmall
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = TimeFormatter.formatRelativeTimestamp(session.startedAt, currentInstant()),
                        style = metaStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (steepCount > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SteepRatingDots(session.steeps.map { it.rating })
                            Text(
                                text = pluralStringResource(Res.plurals.steep_count, steepCount, steepCount),
                                style = metaStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconValue(
                            FeatherIcons.Clock,
                            formatDuration(last.timestamp + last.brewedTime - first.timestamp),
                            metaStyle,
                        )
                    }
                }

                if (tags.isNotEmpty()) TagChips(tags)

                session.session.notes?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val paramStyle = MaterialTheme.typography.labelSmall
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    session.session.teaQuantityGrams?.let {
                        IconValue(vectorResource(Res.drawable.ic_tea_leaf), formatWeight(it), paramStyle)
                    }
                    IconValue(FeatherIcons.Thermometer, formatTemperature(first.temperatureCelsius), paramStyle)
                    IconValue(FeatherIcons.Droplet, formatVolume(first.waterQuantityMl), paramStyle)
                }
            }

            Box(
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
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
                    if (session.session.status == SessionStatus.COMPLETED && canBrewAgain) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.brew_again)) },
                            onClick = {
                                showMenu = false
                                onBrewAgainClick()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.delete)) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun IconValue(icon: ImageVector, text: String, style: TextStyle) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = style.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** A dot per steep, filled from the bottom as far as its rating reaches; a bare ring if unrated. */
@Composable
private fun SteepRatingDots(ratings: List<Float?>) {
    val unrated = stringResource(Res.string.rating_unrated)
    val summary = stringResource(
        Res.string.steep_ratings,
        ratings.joinToString { it?.let(::formatRating) ?: unrated },
    )
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = summary },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ratings.forEach { rating ->
            Canvas(Modifier.size(10.dp)) {
                val stroke = 1.dp.toPx()
                if (rating == null) {
                    drawCircle(color, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
                } else {
                    drawCircle(color.copy(alpha = 0.24f))
                    clipRect(top = size.height * (1 - rating / 5f)) { drawCircle(color) }
                }
            }
        }
    }
}

private val previewTags = listOf("floral", "honey", "returning_sweetness").map { key ->
    TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = CURATED_TAG_KEYS.indexOf(key))
}

@Preview
@Composable
private fun SessionCardCompletedPreview() {
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                ratings = listOf(4f, 4f, 3f, null),
                status = SessionStatus.COMPLETED,
                brewingTime = 30.seconds,
                temperatureCelsius = 95.0,
                teaQuantityGrams = 4f,
                notes = "Perfect brewing time, great flavor profile with hints of honey",
            ),
            teaName = "Dragon Well Green Tea",
            tags = previewTags,
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
        )
    }
}

@Preview
@Composable
private fun SessionCardGongfuPreview() {
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                ratings = listOf(3f, 4f, 5f, 5f, 4f, 4f, 3f, null),
                status = SessionStatus.COMPLETED,
                brewingTime = 20.seconds,
                temperatureCelsius = 99.0,
                waterQuantityMl = 100.0,
                teaQuantityGrams = 7f,
            ),
            teaName = "Da Hong Pao",
            tags = previewTags,
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
        )
    }
}

@Preview
@Composable
private fun SessionCardInProgressPreview() {
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                ratings = listOf(4f, null),
                brewingTime = 4.minutes,
                temperatureCelsius = 100.0,
                waterQuantityMl = 250.0,
                teaQuantityGrams = null,
            ),
            teaName = "English Breakfast",
            tags = emptyList(),
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
        )
    }
}

@Preview
@Composable
private fun SessionCardOneSteepPreview() {
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                status = SessionStatus.COMPLETED,
                brewingTime = 45.seconds,
                temperatureCelsius = 85.0,
                waterQuantityMl = 150.0,
                teaQuantityGrams = null,
                notes = "Quick morning steep, didn't have time to rate",
            ),
            teaName = "Jasmine Silver Needle",
            tags = emptyList(),
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
        )
    }
}

@Preview
@Composable
private fun SessionCardLongNamesPreview() {
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                ratings = listOf(5f),
                status = SessionStatus.COMPLETED,
                temperatureCelsius = 80.0,
                waterQuantityMl = 175.0,
                notes = "This is a very long note that will be truncated with an ellipsis because it exceeds the maximum line limit for the card display, which is two lines rather than one",
            ),
            teaName = "Fujian Province Premium Grade Dragon Phoenix Pearl Green Tea",
            tags = previewTags,
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
        )
    }
}
