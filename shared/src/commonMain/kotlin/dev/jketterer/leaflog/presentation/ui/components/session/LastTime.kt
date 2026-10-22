package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Layers
import compose.icons.feathericons.Thermometer
import compose.icons.feathericons.TrendingUp
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.formatDate
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatRating
import dev.jketterer.leaflog.presentation.ui.components.common.formatSteepIncrement
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.common.ratingDescription
import dev.jketterer.leaflog.presentation.ui.components.common.timeAgo
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.StarFilled
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.best_brew
import leaf_log.shared.generated.resources.brew_again
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.last_time
import leaf_log.shared.generated.resources.steep_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration.Companion.seconds

/**
 * A past session of a tea under [title], such as its last or its best: when, its rating, how
 * steep 1 brewed and, for a gongfu session, how the steeps grew, then its [tags] and the first
 * line of its note. Times come from the plan Brew Again would replay, which [onBrewAgain] starts.
 */
@Composable
fun PastBrewCard(
    title: String,
    session: SessionWithSteeps,
    tags: List<TastingTag>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBrewAgain: (() -> Unit)? = null,
) {
    val plan = remember(session) { BrewingPlan.learnedFrom(session.steeps) }
    val date = session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val note = session.session.notes?.trim()?.lineSequence()?.firstOrNull()?.takeIf { it.isNotEmpty() }

    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = TimeFormatter.formatDate(date, currentDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RatingDisplay(session.effectiveRating)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                session.session.teaQuantityGrams?.let {
                    BrewingParamChip(vectorResource(Res.drawable.ic_tea_leaf), formatWeight(it))
                }
                BrewingParamChip(FeatherIcons.Thermometer, formatTemperature(session.steeps.first().temperatureCelsius))
                BrewingParamChip(FeatherIcons.Clock, formatBrewingTime(plan.base.inWholeSeconds.toInt()))
                plan.increment?.let { step ->
                    BrewingParamChip(FeatherIcons.TrendingUp, formatSteepIncrement(step))
                    val steeps = session.steeps.size
                    BrewingParamChip(FeatherIcons.Layers, pluralStringResource(Res.plurals.steep_count, steeps, steeps))
                }
            }
            if (tags.isNotEmpty()) TagChips(tags)
            note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            onBrewAgain?.let {
                FilledTonalButton(onClick = it, modifier = Modifier.align(Alignment.End)) {
                    Icon(FeatherIcons.Coffee, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.brew_again))
                }
            }
        }
    }
}

/** A tea's last completed session in one line, for a small card: when, and its rating. */
@Composable
fun LastTimeLine(
    session: SessionWithSteeps,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = timeAgo(session.startedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        session.effectiveRating?.let { rating ->
            val description = ratingDescription(rating)
            Row(
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FeatherIcons.StarFilled,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(formatRating(rating), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

private val previewTags = listOf("floral", "returning_sweetness", "smooth").map { key ->
    TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = CURATED_TAG_KEYS.indexOf(key))
}

private fun previewGongfuSession(): SessionWithSteeps {
    val session = previewSession(
        ratings = listOf(4f, 5f, 4.5f, 4f, 3f, 3f),
        status = SessionStatus.COMPLETED,
        temperatureCelsius = 95.0,
        notes = "Went bitter at steep 5, shorter next time\nThe rest was lovely",
    )
    return session.copy(
        steeps = session.steeps.mapIndexed { i, steep -> steep.copy(brewingTime = 10.seconds + 5.seconds * i) },
    )
}

@Preview(showBackground = true)
@Composable
private fun PastBrewCardGongfuPreview() {
    LeafLogTheme {
        PastBrewCard(
            title = stringResource(Res.string.last_time),
            session = previewGongfuSession(),
            tags = previewTags,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PastBrewCardWesternPreview() {
    LeafLogTheme {
        PastBrewCard(
            title = stringResource(Res.string.best_brew),
            session = previewSession(status = SessionStatus.COMPLETED, teaQuantityGrams = null),
            tags = emptyList(),
            onClick = {},
            onBrewAgain = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LastTimeLinePreview() {
    LeafLogTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LastTimeLine(session = previewGongfuSession())
            LastTimeLine(session = previewSession(status = SessionStatus.COMPLETED))
        }
    }
}
