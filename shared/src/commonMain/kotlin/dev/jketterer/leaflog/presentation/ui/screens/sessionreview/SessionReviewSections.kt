package dev.jketterer.leaflog.presentation.ui.screens.sessionreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.usecases.session.BrewDifference
import dev.jketterer.leaflog.domain.usecases.session.VsBest
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.review_best_from
import leaf_log.shared.generated.resources.review_best_session
import leaf_log.shared.generated.resources.review_diff_leaf
import leaf_log.shared.generated.resources.review_diff_temperature
import leaf_log.shared.generated.resources.review_diff_time
import leaf_log.shared.generated.resources.review_diff_vessel
import leaf_log.shared.generated.resources.review_diff_water
import leaf_log.shared.generated.resources.review_diff_water_type
import leaf_log.shared.generated.resources.review_method_label
import leaf_log.shared.generated.resources.review_new_best
import leaf_log.shared.generated.resources.review_note_label
import leaf_log.shared.generated.resources.review_rate_steep
import leaf_log.shared.generated.resources.review_same_as_best
import leaf_log.shared.generated.resources.review_save_method
import leaf_log.shared.generated.resources.review_save_method_header
import leaf_log.shared.generated.resources.review_save_method_text
import leaf_log.shared.generated.resources.review_steep_n
import leaf_log.shared.generated.resources.review_steeps_header
import leaf_log.shared.generated.resources.review_tea_bag
import leaf_log.shared.generated.resources.review_this_session
import leaf_log.shared.generated.resources.review_vs_best_header
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/** One row per steep; tap to edit its rating and note. */
@Composable
internal fun SteepRowsSection(
    steeps: List<Steep>,
    onSteepClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(Res.string.review_steeps_header))
        steeps.forEach { steep ->
            Card(
                onClick = { onSteepClick(steep.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.review_steep_n, steep.number),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = formatBrewingTime(steep.brewedTime.inWholeSeconds.toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    RatingDisplay(rating = steep.rating, iconsOnly = true)
                }
            }
        }
    }
}

/** Only what differs between this session and the tea's best; see [compareWithBest]. */
@Composable
internal fun VsBestSection(
    vsBest: VsBest,
    session: SessionWithSteeps,
    vessel: BrewingVessel?,
    bestVessel: BrewingVessel?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(Res.string.review_vs_best_header))
        when (vsBest) {
            is VsBest.NewBest -> Text(
                text = stringResource(Res.string.review_new_best),
                style = MaterialTheme.typography.bodyLarge,
            )

            is VsBest.Versus -> {
                val date =
                    vsBest.best.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(
                            Res.string.review_best_from,
                            TimeFormatter.formatDate(date, DateSkeleton.MONTH_DAY_YEAR),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RatingDisplay(rating = vsBest.best.effectiveRating, iconsOnly = true)
                }
                if (vsBest.differences.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.review_same_as_best),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        ColumnHeader(stringResource(Res.string.review_this_session))
                        ColumnHeader(stringResource(Res.string.review_best_session))
                    }
                    vsBest.differences.forEach { difference ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = difference.label(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            DifferenceValue(difference.valueOf(session, vessel))
                            DifferenceValue(difference.valueOf(vsBest.best, bestVessel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ColumnHeader(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.weight(1f),
)

@Composable
private fun androidx.compose.foundation.layout.RowScope.DifferenceValue(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    modifier = Modifier.weight(1f),
)

@Composable
private fun BrewDifference.label(): String = stringResource(
    when (this) {
        BrewDifference.LEAF -> Res.string.review_diff_leaf
        BrewDifference.WATER -> Res.string.review_diff_water
        BrewDifference.TEMPERATURE -> Res.string.review_diff_temperature
        BrewDifference.TIME -> Res.string.review_diff_time
        BrewDifference.VESSEL -> Res.string.review_diff_vessel
        BrewDifference.WATER_TYPE -> Res.string.review_diff_water_type
    },
)

@Composable
private fun BrewDifference.valueOf(session: SessionWithSteeps, vessel: BrewingVessel?): String {
    val first = session.steeps.first()
    return when (this) {
        BrewDifference.LEAF -> session.session.teaQuantityGrams?.let { formatWeight(it) }
            ?: stringResource(Res.string.review_tea_bag)

        BrewDifference.WATER -> formatVolume(first.waterQuantityMl)
        BrewDifference.TEMPERATURE -> formatTemperature(first.temperatureCelsius)
        BrewDifference.TIME -> formatBrewingTime(first.brewingTime.inWholeSeconds.toInt())
        BrewDifference.VESSEL -> vessel?.name ?: "-"
        BrewDifference.WATER_TYPE -> session.session.waterType.name.lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}

/** Offered once; the card goes away when the method is saved. */
@Composable
internal fun SaveMethodCard(
    label: String,
    onLabelChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.review_save_method_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.review_save_method_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                label = { Text(stringResource(Res.string.review_method_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
            Button(onClick = onSave, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.review_save_method))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SteepEditSheet(
    rating: Float,
    notes: String,
    onRatingChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SteepEditSheetContent(rating, notes, onRatingChange, onNotesChange)
    }
}

@Composable
private fun SteepEditSheetContent(
    rating: Float,
    notes: String,
    onRatingChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.review_rate_steep),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RatingSelector(
            rating = rating,
            onRatingChange = onRatingChange,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(Res.string.review_note_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepRowsSectionPreview() {
    LeafLogTheme {
        SteepRowsSection(
            steeps = previewSession(
                ratings = listOf(4f, 4.5f, null),
                brewingTime = 25.seconds
            ).steeps,
            onSteepClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VsBestSectionPreview() {
    val mine = previewSession(id = "mine", ratings = listOf(3.5f), temperatureCelsius = 90.0)
    val best = previewSession(id = "best", ratings = listOf(4.5f), temperatureCelsius = 80.0)
    LeafLogTheme {
        VsBestSection(
            vsBest = VsBest.Versus(best, listOf(BrewDifference.TEMPERATURE)),
            session = mine,
            vessel = null,
            bestVessel = null,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SaveMethodCardPreview() {
    LeafLogTheme {
        SaveMethodCard(
            label = "5g · 100ml · 25s",
            onLabelChange = {},
            onSave = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepEditSheetPreview() {
    LeafLogTheme {
        SheetPreviewContainer {
            SteepEditSheetContent(
                rating = 4f,
                notes = "Sweet finish",
                onRatingChange = {},
                onNotesChange = {})
        }
    }
}
