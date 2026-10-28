package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Play
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.SteepTag
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.dominantTags
import dev.jketterer.leaflog.domain.models.formatDate
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.adjust
import leaf_log.shared.generated.resources.best_brew
import leaf_log.shared.generated.resources.brew_again
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.last_time
import leaf_log.shared.generated.resources.plan_adjusted
import leaf_log.shared.generated.resources.plan_steep_label
import leaf_log.shared.generated.resources.review_note_header
import leaf_log.shared.generated.resources.review_steeps_header
import leaf_log.shared.generated.resources.review_tea_bag
import leaf_log.shared.generated.resources.rinsed
import leaf_log.shared.generated.resources.start_brewing
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A past [brew] of a tea in a vessel, shown in full before brewing it again: [onStart] replays it,
 * [onAdjust] opens it in Log Tea. Given a pair's [last] and [best], a switch chooses between them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewAgainSheet(
    teaName: String,
    teaTypeColorHex: String?,
    vesselName: String,
    brew: SessionWithSteeps,
    tagVocabulary: List<TastingTag>,
    onStart: () -> Unit,
    onAdjust: () -> Unit,
    onDismiss: () -> Unit,
    last: SessionWithSteeps? = null,
    best: SessionWithSteeps? = null,
    onSelect: (sessionId: String) -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        BrewAgainContent(
            teaName = teaName,
            teaTypeColorHex = teaTypeColorHex,
            vesselName = vesselName,
            brew = brew,
            tagVocabulary = tagVocabulary,
            onStart = onStart,
            onAdjust = onAdjust,
            last = last,
            best = best,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun BrewAgainContent(
    teaName: String,
    teaTypeColorHex: String?,
    vesselName: String,
    brew: SessionWithSteeps,
    tagVocabulary: List<TastingTag>,
    onStart: () -> Unit,
    onAdjust: () -> Unit,
    last: SessionWithSteeps?,
    best: SessionWithSteeps?,
    onSelect: (sessionId: String) -> Unit,
) {
    val teaColor = teaTypeColorHex?.let { teaTypeColor(it) }?.takeIf { it.isSpecified }
        ?: MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TeaHeader(teaName = teaName, vesselName = vesselName, teaColor = teaColor)
            Text(
                text = stringResource(Res.string.brew_again),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        if (last != null && best != null) {
            val choices = listOf(last to Res.string.last_time, best to Res.string.best_brew)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                choices.forEachIndexed { index, (choice, label) ->
                    SegmentedButton(
                        selected = choice.session.id == brew.session.id,
                        onClick = { onSelect(choice.session.id) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
                    ) {
                        Text(stringResource(label), maxLines = 1)
                    }
                }
            }
        }
        AnimatedContent(targetState = brew, contentKey = { it.session.id }) { shown ->
            BrewDetails(shown, remember(shown, tagVocabulary) { shown.dominantTags(tagVocabulary) })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onAdjust, modifier = Modifier.weight(1f)) {
                Icon(FeatherIcons.Edit2, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.adjust))
            }
            Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                Icon(FeatherIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.start_brewing))
            }
        }
    }
}

/**
 * When it was, how it went and how it was set up, as Session Detail opens; then steep 1's
 * parameters and each steep's time as Start replays it; then the note's first line.
 */
@Composable
private fun BrewDetails(brew: SessionWithSteeps, tags: List<TastingTag>) {
    val plan = remember(brew) { BrewingPlan.learnedFrom(brew.steeps) }
    val first = brew.steeps.first()
    val date = brew.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val note = brew.session.notes?.trim()?.lineSequence()?.firstOrNull()?.takeIf { it.isNotEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = TimeFormatter.formatDate(date, currentDate()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RatingDisplay(brew.effectiveRating)
            if (tags.isNotEmpty()) TagChips(tags)
            val style = MaterialTheme.typography.bodyMedium
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconValue(
                    icon = vectorResource(Res.drawable.ic_tea_leaf),
                    text = brew.session.teaQuantityGrams?.let { formatWeight(it) }
                        ?: stringResource(Res.string.review_tea_bag),
                    style = style,
                    iconSize = 16.dp,
                )
                IconValue(FeatherIcons.Droplet, brew.session.waterType.displayName, style, iconSize = 16.dp)
                if (brew.session.rinsed) {
                    IconValue(FeatherIcons.RefreshCw, stringResource(Res.string.rinsed), style, iconSize = 16.dp)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(stringResource(Res.string.review_steeps_header))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (brew.steeps.size == 1) {
                    BrewingParamChip(FeatherIcons.Clock, formatBrewingTime(plan.base.inWholeSeconds.toInt()))
                }
                BrewingParamChip(FeatherIcons.Thermometer, formatTemperature(first.temperatureCelsius))
                BrewingParamChip(FeatherIcons.Droplet, formatVolume(first.waterQuantityMl))
            }
            if (brew.steeps.size > 1) {
                val steeps = (1..brew.steeps.size).map { it to plan.timeOf(it) }
                val longest = steeps.maxOf { it.second }
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    steeps.forEach { (steep, time) ->
                        PlanRow(steep, time, fraction = (time / longest).toFloat(), adjusted = steep in plan.overrides)
                    }
                }
            }
        }
        note?.let {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(Res.string.review_note_header))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One steep: its number, a bar sized against the longest steep shown, its time and an Adjusted tag. */
@Composable
internal fun PlanRow(steep: Int, time: Duration, fraction: Float, adjusted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.plan_steep_label, steep),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(64.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(12.dp)
                    .background(
                        color = if (adjusted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                    ),
            )
        }
        Text(
            text = formatBrewingTime(time.inWholeSeconds.toInt()),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text = if (adjusted) stringResource(Res.string.plan_adjusted) else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(60.dp),
        )
    }
}

private val previewVocabulary = CURATED_TAG_KEYS.mapIndexed { i, key ->
    TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = i)
}

/** A gongfu brew whose steeps grow by 5s from 10s, each tagged with [tags]. */
private fun previewGongfu(id: String, ratings: List<Float?>, tags: List<String> = emptyList()): SessionWithSteeps {
    val session = previewSession(
        id = id,
        ratings = ratings,
        status = SessionStatus.COMPLETED,
        temperatureCelsius = 95.0,
        waterQuantityMl = 100.0,
        notes = "Sweet from the second steep\nWent flat after six",
    )
    return session.copy(
        steeps = session.steeps.mapIndexed { i, steep ->
            steep.copy(
                brewingTime = 10.seconds + 5.seconds * i,
                tags = tags.map { SteepTag(it, confirmed = true) },
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun BrewAgainPairPreview() {
    val last = previewSession(id = "last", ratings = listOf(2f), status = SessionStatus.COMPLETED)
    val best = previewGongfu("best", listOf(5f, 4.5f, 4.5f, 4f), tags = listOf("floral", "smooth"))
    LeafLogTheme {
        SheetPreviewContainer(height = 720.dp) {
            BrewAgainContent(
                teaName = "Tie Guan Yin",
                teaTypeColorHex = "#C27400",
                vesselName = "Gaiwan",
                brew = best,
                tagVocabulary = previewVocabulary,
                onStart = {},
                onAdjust = {},
                last = last,
                best = best,
                onSelect = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewAgainSessionPreview() {
    LeafLogTheme {
        SheetPreviewContainer(height = 640.dp) {
            BrewAgainContent(
                teaName = "Da Hong Pao",
                teaTypeColorHex = "#8D4E2A",
                vesselName = "Yixing pot",
                brew = previewGongfu("session", listOf(4f, 4f, 3.5f)),
                tagVocabulary = previewVocabulary,
                onStart = {},
                onAdjust = {},
                last = null,
                best = null,
                onSelect = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewAgainWesternPreview() {
    val session = previewSession(
        ratings = listOf(4f),
        status = SessionStatus.COMPLETED,
        brewingTime = 3.minutes,
        temperatureCelsius = 100.0,
        waterQuantityMl = 350.0,
        teaQuantityGrams = null,
    )
    LeafLogTheme {
        SheetPreviewContainer(height = 420.dp) {
            BrewAgainContent(
                teaName = "English Breakfast",
                teaTypeColorHex = "#B71C1C",
                vesselName = "Mug",
                brew = session.copy(session = session.session.copy(waterType = WaterType.TAP)),
                tagVocabulary = previewVocabulary,
                onStart = {},
                onAdjust = {},
                last = null,
                best = null,
                onSelect = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewAgainGongfuPreview() {
    val session = previewGongfu("gongfu", listOf(5f, 5f, 4.5f, 4.5f, 4f, 4f, 3.5f, 3f), tags = listOf("returning_sweetness"))
    val rinsed = session.copy(
        session = session.session.copy(rinsed = true),
        // Steep 6 was left long, an override the replay keeps
        steeps = session.steeps.map { if (it.number == 6) it.copy(brewingTime = 1.minutes) else it },
    )
    LeafLogTheme {
        SheetPreviewContainer(height = 760.dp) {
            BrewAgainContent(
                teaName = "Lao Ban Zhang Sheng Pu-erh",
                teaTypeColorHex = "#5D4037",
                vesselName = "Gaiwan",
                brew = rinsed,
                tagVocabulary = previewVocabulary,
                onStart = {},
                onAdjust = {},
                last = null,
                best = null,
                onSelect = {},
            )
        }
    }
}
