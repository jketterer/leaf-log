package dev.jketterer.leaflog.presentation.ui.screens.sessionreview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Award
import compose.icons.feathericons.Check
import compose.icons.feathericons.Droplet
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SteepTag
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.usecases.session.BrewDifference
import dev.jketterer.leaflog.domain.usecases.session.VsBest
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.SteepTagChips
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.review_best_session
import leaf_log.shared.generated.resources.review_diff_leaf
import leaf_log.shared.generated.resources.review_diff_temperature
import leaf_log.shared.generated.resources.review_diff_time
import leaf_log.shared.generated.resources.review_diff_vessel
import leaf_log.shared.generated.resources.review_diff_water
import leaf_log.shared.generated.resources.review_diff_water_type
import leaf_log.shared.generated.resources.review_method_label
import leaf_log.shared.generated.resources.review_method_saved
import leaf_log.shared.generated.resources.review_method_saved_detail
import leaf_log.shared.generated.resources.review_new_best
import leaf_log.shared.generated.resources.review_same_as_best
import leaf_log.shared.generated.resources.review_save_method
import leaf_log.shared.generated.resources.review_save_method_header
import leaf_log.shared.generated.resources.review_save_method_text
import leaf_log.shared.generated.resources.review_steep_n
import leaf_log.shared.generated.resources.review_steeps_header
import leaf_log.shared.generated.resources.review_tea_bag
import leaf_log.shared.generated.resources.review_this_session
import leaf_log.shared.generated.resources.review_update_method
import leaf_log.shared.generated.resources.review_update_method_header
import leaf_log.shared.generated.resources.review_update_method_text
import leaf_log.shared.generated.resources.review_vs_best_header
import leaf_log.shared.generated.resources.rinsed
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/** One row per steep; tap to edit its rating, tags and note. */
@Composable
internal fun SteepRowsSection(
    steeps: List<Steep>,
    vocabulary: List<TastingTag>,
    rinsed: Boolean,
    onSteepClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = stringResource(Res.string.review_steeps_header),
            action = if (rinsed) {
                {
                    BrewingParamChip(
                        icon = FeatherIcons.Droplet,
                        text = stringResource(Res.string.rinsed)
                    )
                }
            } else null,
        )
        steeps.forEach { steep ->
            Card(
                onClick = { onSteepClick(steep.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                    SteepTagChips(steep.tags, vocabulary, modifier = Modifier.padding(top = 8.dp))
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
            is VsBest.NewBest -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        FeatherIcons.Award,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = stringResource(Res.string.review_new_best),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            is VsBest.Versus -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        RatingColumn(
                            stringResource(Res.string.review_this_session),
                            session.effectiveRating
                        )
                        RatingColumn(
                            stringResource(Res.string.review_best_session),
                            vsBest.best.effectiveRating
                        )
                    }
                    HorizontalDivider()
                    if (vsBest.differences.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                FeatherIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(Res.string.review_same_as_best),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    } else {
                        vsBest.differences.forEach { difference ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = difference.label(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                DifferenceValue(
                                    difference.valueOf(session, vessel),
                                    emphasized = true
                                )
                                DifferenceValue(difference.valueOf(vsBest.best, bestVessel))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.RatingColumn(title: String, rating: Float?) = Column(
    modifier = Modifier.weight(1f),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    RatingDisplay(rating = rating, iconsOnly = true)
}

@Composable
private fun RowScope.DifferenceValue(text: String, emphasized: Boolean = false) = Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    fontWeight = if (emphasized) FontWeight.Bold else null,
    color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
        BrewDifference.TIME -> formatBrewingTime(first.brewedTime.inWholeSeconds.toInt())
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
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onSave()
                }),
            )
            Button(onClick = onSave, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.review_save_method))
            }
        }
    }
}

/** The card that replaces a method offer once it's acted on; the badge pops in. */
@Composable
internal fun MethodDoneCard(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedVisibility(
                visibleState = visible,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        FeatherIcons.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** Offered once, when the brew deviated from the method it used; the card goes away when updated. */
@Composable
internal fun UpdateMethodCard(
    methodName: String,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.review_update_method_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.review_update_method_text, methodName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onUpdate, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.review_update_method))
            }
        }
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
            ).steeps.mapIndexed { i, steep ->
                steep.copy(tags = listOf(SteepTag("floral", confirmed = i == 0)))
            },
            vocabulary = listOf(
                TastingTag(
                    "floral",
                    key = "floral",
                    name = null,
                    isCustom = false,
                    displayOrder = 0
                )
            ),
            rinsed = true,
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
private fun VsBestSectionNewBestPreview() {
    LeafLogTheme {
        VsBestSection(
            vsBest = VsBest.NewBest,
            session = previewSession(ratings = listOf(5f)),
            vessel = null,
            bestVessel = null,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VsBestSectionSamePreview() {
    LeafLogTheme {
        VsBestSection(
            vsBest = VsBest.Versus(
                previewSession(id = "best", ratings = listOf(4.5f)),
                emptyList()
            ),
            session = previewSession(ratings = listOf(3.5f)),
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
private fun MethodDoneCardPreview() {
    LeafLogTheme {
        MethodDoneCard(
            title = stringResource(Res.string.review_method_saved),
            detail = stringResource(Res.string.review_method_saved_detail),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateMethodCardPreview() {
    LeafLogTheme {
        UpdateMethodCard(
            methodName = "Morning gaiwan",
            onUpdate = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
