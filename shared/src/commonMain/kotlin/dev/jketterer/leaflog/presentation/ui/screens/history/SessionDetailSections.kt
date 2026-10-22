package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bookmark
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Pause
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SteepTag
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.formatDate
import dev.jketterer.leaflog.domain.models.TimelineSteep
import dev.jketterer.leaflog.domain.models.formatClockTime
import dev.jketterer.leaflog.domain.models.timeline
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoGrid
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatDuration
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.IconValue
import dev.jketterer.leaflog.presentation.ui.components.session.TagChips
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.date_at_time
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.review_steep_n
import leaf_log.shared.generated.resources.review_steeps_header
import leaf_log.shared.generated.resources.review_tea_bag
import leaf_log.shared.generated.resources.rinsed
import leaf_log.shared.generated.resources.session_break
import leaf_log.shared.generated.resources.session_total_brew_time
import leaf_log.shared.generated.resources.tea_not_found
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The tea, when, how it rated, how it was set up, the session's note and its photos. */
@Composable
internal fun SessionHeaderSection(
    teaName: String?,
    onTeaClick: (() -> Unit)?,
    startedAt: Instant,
    rating: Float?,
    session: Session,
    vesselName: String?,
    methodLabel: String?,
    photos: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = teaName ?: stringResource(Res.string.tea_not_found),
            style = MaterialTheme.typography.headlineSmall,
            color = if (onTeaClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .then(
                    if (onTeaClick != null) Modifier.clickable(role = Role.Button, onClick = onTeaClick)
                    else Modifier
                )
                .heightIn(min = 48.dp)
                .wrapContentHeight(Alignment.CenterVertically),
        )

        val dateTime = startedAt.toLocalDateTime(TimeZone.currentSystemDefault())
        Text(
            text = stringResource(
                Res.string.date_at_time,
                TimeFormatter.formatDate(dateTime.date, currentDate()),
                TimeFormatter.formatClockTime(dateTime.hour, dateTime.minute),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RatingDisplay(rating)

        val style = MaterialTheme.typography.bodyMedium
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            vesselName?.let { IconValue(FeatherIcons.Coffee, it, style, iconSize = 16.dp) }
            IconValue(FeatherIcons.Droplet, session.waterType.displayName, style, iconSize = 16.dp)
            IconValue(
                icon = vectorResource(Res.drawable.ic_tea_leaf),
                text = session.teaQuantityGrams?.let { formatWeight(it) } ?: stringResource(Res.string.review_tea_bag),
                style = style,
                iconSize = 16.dp,
            )
            if (session.rinsed) {
                IconValue(FeatherIcons.RefreshCw, stringResource(Res.string.rinsed), style, iconSize = 16.dp)
            }
            session.location?.let { IconValue(FeatherIcons.MapPin, it, style, iconSize = 16.dp) }
            methodLabel?.let { IconValue(FeatherIcons.Bookmark, it, style, iconSize = 16.dp) }
        }

        session.notes?.let {
            Text(text = it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
        }

        if (photos.isNotEmpty()) {
            PhotoGrid(photos = photos, onAddPhoto = {}, onRemovePhoto = {}, enabled = false)
        }
    }
}

/** Each steep on a rail at the time it started, a break where the rest ran long, then the total. */
@Composable
internal fun SteepTimelineSection(
    timeline: List<TimelineSteep>,
    totalBrewTime: Duration,
    vocabulary: List<TastingTag>,
    onSteepClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(stringResource(Res.string.review_steeps_header))
        timeline.forEachIndexed { index, item ->
            item.breakBefore?.let { TimelineBreak(it) }
            val confirmed = item.steep.tags.filter { it.confirmed }.map { it.tagId }.toSet()
            TimelineSteepRow(
                item = item,
                tags = vocabulary.filter { it.id in confirmed },
                isFirst = index == 0,
                isLast = index == timeline.lastIndex,
                onClick = { onSteepClick(item.steep.id) },
            )
        }
        IconValue(
            icon = FeatherIcons.Clock,
            text = stringResource(
                Res.string.session_total_brew_time,
                formatBrewingTime(totalBrewTime.inWholeSeconds.toInt()),
            ),
            style = MaterialTheme.typography.bodyMedium,
            iconSize = 16.dp,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )
    }
}

private val RAIL_X = 12.dp
private val DOT_Y = 18.dp

/** Its clock time, number and rating, what changed from the steep before, its tags and note. */
@Composable
private fun TimelineSteepRow(
    item: TimelineSteep,
    tags: List<TastingTag>,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val steep = item.steep
    val railColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .drawBehind {
                val x = RAIL_X.toPx()
                val dotY = DOT_Y.toPx()
                drawLine(
                    color = railColor,
                    start = Offset(x, if (isFirst) dotY else 0f),
                    end = Offset(x, if (isLast) dotY else size.height),
                    strokeWidth = 2.dp.toPx(),
                )
                drawCircle(dotColor, radius = 5.dp.toPx(), center = Offset(x, dotY))
            }
            .padding(start = 32.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = TimeFormatter.formatClockTime(steep.timestamp),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.review_steep_n, steep.number),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            RatingDisplay(rating = steep.rating, iconsOnly = true)
        }
        val style = MaterialTheme.typography.bodySmall
        if (item.brewedTime != null || item.temperatureCelsius != null || item.waterQuantityMl != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item.brewedTime?.let { IconValue(FeatherIcons.Clock, formatBrewingTime(it.inWholeSeconds.toInt()), style) }
                item.temperatureCelsius?.let { IconValue(FeatherIcons.Thermometer, formatTemperature(it), style) }
                item.waterQuantityMl?.let { IconValue(FeatherIcons.Droplet, formatVolume(it), style) }
            }
        }
        if (tags.isNotEmpty()) TagChips(tags)
        steep.notes?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The rail dashed through a long rest, with how long it was. */
@Composable
private fun TimelineBreak(duration: Duration) {
    val railColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = RAIL_X.toPx()
                val dash = 4.dp.toPx()
                drawLine(
                    color = railColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
            }
            .padding(start = 32.dp, top = 8.dp, bottom = 8.dp),
    ) {
        IconValue(
            icon = FeatherIcons.Pause,
            text = stringResource(Res.string.session_break, formatDuration(duration)),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionHeaderSectionPreview() {
    val session = previewSession(ratings = listOf(4f, 5f), notes = "Opened up beautifully by the third steep")
    LeafLogTheme {
        SessionHeaderSection(
            teaName = "Da Hong Pao",
            onTeaClick = {},
            startedAt = session.startedAt,
            rating = session.effectiveRating,
            session = session.session.copy(rinsed = true, location = "Home"),
            vesselName = "Gaiwan",
            methodLabel = "Gongfu 7g",
            photos = emptyList(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepTimelineSectionPreview() {
    val base = previewSession(ratings = listOf(4f, 5f, 4f, null), brewingTime = 20.seconds)
    val session = base.copy(
        steeps = base.steeps.mapIndexed { index, steep ->
            when (index) {
                0 -> steep.copy(tags = listOf(SteepTag("floral", true), SteepTag("honey", true)))
                1 -> steep.copy(brewingTime = 25.seconds, tags = listOf(SteepTag("floral", false)))
                2 -> steep.copy(brewingTime = 35.seconds, notes = "Getting woody")
                else -> steep.copy(brewingTime = 50.seconds, temperatureCelsius = 95.0, timestamp = steep.timestamp + 15.minutes)
            }
        },
    )
    val vocabulary = listOf("floral", "honey").map {
        TastingTag(id = it, key = it, name = null, isCustom = false, displayOrder = CURATED_TAG_KEYS.indexOf(it))
    }
    LeafLogTheme {
        SteepTimelineSection(
            timeline = session.timeline(),
            totalBrewTime = session.totalBrewTime,
            vocabulary = vocabulary,
            onSteepClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
