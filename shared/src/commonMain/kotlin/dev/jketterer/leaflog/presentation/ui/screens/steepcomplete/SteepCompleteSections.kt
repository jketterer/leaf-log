package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.RotateCcw
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SteepTag
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.formatDate
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.IconValue
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.components.session.SteepTags
import dev.jketterer.leaflog.presentation.ui.components.session.TagChips
import dev.jketterer.leaflog.presentation.ui.components.session.TeaEyebrow
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.edit_parameters
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.review_add_note
import leaf_log.shared.generated.resources.review_how_was_steep_n
import leaf_log.shared.generated.resources.review_rating
import leaf_log.shared.generated.resources.review_tasting_notes
import leaf_log.shared.generated.resources.steep_n_on_date
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration.Companion.seconds

/** Which tea and vessel, the question it asks of this steep, then the steep's parameters, tapped to change them. */
@Composable
internal fun SteepHeaderSection(
    teaName: String,
    vesselName: String?,
    teaColor: Color,
    steep: Steep,
    teaQuantityGrams: Float?,
    onEditParameters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TeaEyebrow(teaName = teaName, vesselName = vesselName, teaColor = teaColor)
        Text(
            text = stringResource(Res.string.review_how_was_steep_n, steep.number),
            style = MaterialTheme.typography.displaySmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BrewingParamChip(FeatherIcons.Clock, formatBrewingTime(steep.brewedTime.inWholeSeconds.toInt()))
                BrewingParamChip(FeatherIcons.Thermometer, formatTemperature(steep.temperatureCelsius))
                BrewingParamChip(FeatherIcons.Droplet, formatVolume(steep.waterQuantityMl))
                teaQuantityGrams?.let { BrewingParamChip(vectorResource(Res.drawable.ic_tea_leaf), formatWeight(it)) }
            }
            IconButton(onClick = onEditParameters) {
                Icon(
                    imageVector = FeatherIcons.Edit2,
                    contentDescription = stringResource(Res.string.edit_parameters),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The answer: stars, the vocabulary's tags ready to tap, and a note, each under its label. */
@Composable
internal fun SteepReviewSection(
    steep: Steep,
    rating: Float,
    notes: String,
    vocabulary: List<TastingTag>,
    suggestedTags: List<TastingTag>,
    onRatingChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onTagCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(Res.string.review_rating), style = MaterialTheme.typography.titleSmall)
            RatingSelector(
                rating = rating,
                onRatingChange = onRatingChange,
                modifier = Modifier.fillMaxWidth(),
                starSize = 32.dp,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(Res.string.review_tasting_notes), style = MaterialTheme.typography.titleSmall)
            SteepTags(
                tags = steep.tags,
                vocabulary = vocabulary,
                onToggle = onTagToggle,
                onCreate = onTagCreate,
                modifier = Modifier.fillMaxWidth(),
                suggestions = suggestedTags,
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            placeholder = { Text(stringResource(Res.string.review_add_note)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 6,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
    }
}

/**
 * The same steep from an earlier session, to rate this one against: when, its rating, time and
 * temperature, its confirmed tags and the first line of its note.
 */
@Composable
internal fun ComparisonSteepSection(
    steep: Steep,
    vocabulary: List<TastingTag>,
    modifier: Modifier = Modifier,
) {
    val date = steep.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val confirmed = steep.tags.filter { it.confirmed }.map { it.tagId }.toSet()
    val tags = vocabulary.filter { it.id in confirmed }
    val note = steep.notes?.trim()?.lineSequence()?.firstOrNull()?.takeIf { it.isNotEmpty() }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = FeatherIcons.RotateCcw,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.steep_n_on_date, steep.number, TimeFormatter.formatDate(date, currentDate())),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            RatingDisplay(steep.rating)
        }
        Column(
            modifier = Modifier.padding(start = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val style = MaterialTheme.typography.bodyMedium
                IconValue(FeatherIcons.Clock, formatBrewingTime(steep.brewedTime.inWholeSeconds.toInt()), style, iconSize = 14.dp)
                IconValue(FeatherIcons.Thermometer, formatTemperature(steep.temperatureCelsius), style, iconSize = 14.dp)
            }
            if (tags.isNotEmpty()) TagChips(tags)
            note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal val previewVocabulary = CURATED_TAG_KEYS.map { key ->
    TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = CURATED_TAG_KEYS.indexOf(key))
}

/** An earlier session whose steeps are rated and tagged, each with a two-line note. */
internal fun previewComparison() =
    previewSession(ratings = listOf(4f, 4.5f, 3.5f), status = SessionStatus.COMPLETED).let { session ->
        session.copy(
            steeps = session.steeps.map {
                it.copy(
                    tags = listOf(SteepTag("floral", confirmed = true), SteepTag("honey", confirmed = false)),
                    notes = "Opened up, sweeter than the first\nA little thin by the end",
                )
            },
        )
    }

private val previewSteep = previewSession(brewingTime = 45.seconds).steeps.first()

@Preview(showBackground = true)
@Composable
private fun SteepHeaderSectionPreview() {
    LeafLogTheme {
        SteepHeaderSection(
            teaName = "Tie Guan Yin",
            vesselName = "Gaiwan",
            teaColor = Color(0xFFC27400),
            steep = previewSteep,
            teaQuantityGrams = 5f,
            onEditParameters = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepHeaderSectionLongNamesPreview() {
    LeafLogTheme {
        SteepHeaderSection(
            teaName = "Lishan High Mountain Winter Harvest Oolong",
            vesselName = "Yixing Zhuni Xi Shi Teapot",
            teaColor = Color(0xFFC27400),
            steep = previewSteep,
            teaQuantityGrams = 5f,
            onEditParameters = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepReviewSectionPreview() {
    LeafLogTheme {
        SteepReviewSection(
            steep = previewSteep.copy(tags = listOf(SteepTag("floral", confirmed = true))),
            rating = 4f,
            notes = "",
            vocabulary = previewVocabulary,
            suggestedTags = previewVocabulary.take(TAG_SUGGESTIONS),
            onRatingChange = {},
            onNotesChange = {},
            onTagToggle = {},
            onTagCreate = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ComparisonSteepSectionPreview() {
    LeafLogTheme {
        ComparisonSteepSection(
            steep = previewComparison().steeps.last(),
            vocabulary = previewVocabulary,
            modifier = Modifier.padding(24.dp),
        )
    }
}
