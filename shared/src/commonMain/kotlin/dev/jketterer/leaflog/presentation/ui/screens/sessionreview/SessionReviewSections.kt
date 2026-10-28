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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Award
import compose.icons.feathericons.Check
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.formatDate
import dev.jketterer.leaflog.domain.usecases.session.BrewDifference
import dev.jketterer.leaflog.domain.usecases.session.VsBest
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatRating
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.IconValue
import dev.jketterer.leaflog.presentation.ui.components.session.TeaEyebrow
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.review_best_from
import leaf_log.shared.generated.resources.review_diff_leaf
import leaf_log.shared.generated.resources.review_diff_temperature
import leaf_log.shared.generated.resources.review_diff_time
import leaf_log.shared.generated.resources.review_diff_vessel
import leaf_log.shared.generated.resources.review_diff_water
import leaf_log.shared.generated.resources.review_diff_water_type
import leaf_log.shared.generated.resources.review_matched_best
import leaf_log.shared.generated.resources.review_method_label
import leaf_log.shared.generated.resources.review_method_saved
import leaf_log.shared.generated.resources.review_method_saved_detail
import leaf_log.shared.generated.resources.review_new_best
import leaf_log.shared.generated.resources.review_same_as_best
import leaf_log.shared.generated.resources.review_save_method
import leaf_log.shared.generated.resources.review_save_method_header
import leaf_log.shared.generated.resources.review_save_method_text
import leaf_log.shared.generated.resources.review_session_complete
import leaf_log.shared.generated.resources.review_tea_bag
import leaf_log.shared.generated.resources.review_this_session
import leaf_log.shared.generated.resources.review_update_method
import leaf_log.shared.generated.resources.review_update_method_header
import leaf_log.shared.generated.resources.review_update_method_text
import leaf_log.shared.generated.resources.review_vs_best_header
import leaf_log.shared.generated.resources.rinsed
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Which tea and vessel, that the session is done, how it rated and whether that's a new best for
 * the tea, then how it was set up.
 */
@Composable
internal fun SessionReviewHeaderSection(
    teaName: String,
    vesselName: String?,
    teaColor: Color,
    rating: Float?,
    isNewBest: Boolean,
    session: Session,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TeaEyebrow(teaName = teaName, vesselName = vesselName, teaColor = teaColor)
        Text(
            text = stringResource(Res.string.review_session_complete),
            style = MaterialTheme.typography.displaySmall,
        )
        RatingDisplay(rating)
        if (isNewBest) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = FeatherIcons.Award,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(Res.string.review_new_best),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        val style = MaterialTheme.typography.bodyMedium
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconValue(
                icon = vectorResource(Res.drawable.ic_tea_leaf),
                text = session.teaQuantityGrams?.let { formatWeight(it) } ?: stringResource(Res.string.review_tea_bag),
                style = style,
                iconSize = 16.dp,
            )
            IconValue(FeatherIcons.Droplet, session.waterType.displayName, style, iconSize = 16.dp)
            if (session.rinsed) {
                IconValue(FeatherIcons.RefreshCw, stringResource(Res.string.rinsed), style, iconSize = 16.dp)
            }
        }
    }
}

/**
 * This session's rating over the tea's best, then only what the brewing differed in, each as this
 * session's value toward the best's; see [compareWithBest].
 */
@Composable
internal fun VsBestSection(
    versus: VsBest.Versus,
    session: SessionWithSteeps,
    vessel: BrewingVessel?,
    bestVessel: BrewingVessel?,
    modifier: Modifier = Modifier,
) {
    val rating = session.effectiveRating
    val bestRating = versus.best.effectiveRating
    val bestDate = versus.best.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(Res.string.review_vs_best_header),
            // Compared as shown, so two ratings that read the same match
            action = if (rating != null && bestRating != null && formatRating(rating) == formatRating(bestRating)) {
                { CheckLine(stringResource(Res.string.review_matched_best), MaterialTheme.typography.labelLarge) }
            } else null,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RatingRow(stringResource(Res.string.review_this_session), rating)
            RatingRow(
                stringResource(Res.string.review_best_from, TimeFormatter.formatDate(bestDate, currentDate())),
                bestRating,
            )
        }
        HorizontalDivider()
        if (versus.differences.isEmpty()) {
            CheckLine(stringResource(Res.string.review_same_as_best), MaterialTheme.typography.bodyLarge)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                versus.differences.forEach { difference ->
                    DifferenceRow(
                        icon = difference.icon(),
                        label = difference.label(),
                        mine = difference.valueOf(session, vessel),
                        best = difference.valueOf(versus.best, bestVessel),
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingRow(label: String, rating: Float?) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
) {
    Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    RatingDisplay(rating)
}

@Composable
private fun CheckLine(text: String, style: TextStyle) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    Icon(
        imageVector = FeatherIcons.Check,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Text(text = text, style = style, color = MaterialTheme.colorScheme.primary)
}

/** The parameter, then this session's value toward the best's, which wraps rather than crowd the label. */
@Composable
private fun DifferenceRow(icon: ImageVector, label: String, mine: String, best: String) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(text = label, style = MaterialTheme.typography.bodyMedium)
    Text(
        text = buildAnnotatedString {
            append("$mine  →  ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append(best)
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun BrewDifference.icon(): ImageVector = when (this) {
    BrewDifference.LEAF -> vectorResource(Res.drawable.ic_tea_leaf)
    BrewDifference.WATER -> FeatherIcons.Droplet
    BrewDifference.TEMPERATURE -> FeatherIcons.Thermometer
    BrewDifference.TIME -> FeatherIcons.Clock
    BrewDifference.VESSEL -> FeatherIcons.Coffee
    BrewDifference.WATER_TYPE -> FeatherIcons.Filter
}

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
        BrewDifference.WATER_TYPE -> session.session.waterType.displayName
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
            modifier = Modifier.padding(24.dp),
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
            modifier = Modifier.padding(24.dp),
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
            modifier = Modifier.padding(24.dp),
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
private fun SessionReviewHeaderSectionPreview() {
    val session = previewSession(ratings = listOf(4f, 4.5f, null))
    LeafLogTheme {
        SessionReviewHeaderSection(
            teaName = "Tie Guan Yin",
            vesselName = "Gaiwan",
            teaColor = Color(0xFFC27400),
            rating = session.effectiveRating,
            isNewBest = false,
            session = session.session.copy(rinsed = true),
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionReviewHeaderSectionNewBestPreview() {
    val session = previewSession(ratings = listOf(5f))
    LeafLogTheme {
        SessionReviewHeaderSection(
            teaName = "Dragon Well",
            vesselName = null,
            teaColor = Color(0xFF4CAF50),
            rating = session.effectiveRating,
            isNewBest = true,
            session = session.session,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VsBestSectionPreview() {
    val mine = previewSession(id = "mine", ratings = listOf(3.5f), temperatureCelsius = 90.0, brewingTime = 2.minutes)
    val best = previewSession(id = "best", ratings = listOf(4.5f), temperatureCelsius = 80.0, brewingTime = 150.seconds)
    LeafLogTheme {
        VsBestSection(
            versus = VsBest.Versus(
                best,
                listOf(BrewDifference.TEMPERATURE, BrewDifference.TIME, BrewDifference.VESSEL),
            ),
            session = mine,
            vessel = previewVessel("Yixing Zhuni Xi Shi Teapot"),
            bestVessel = previewVessel("Gaiwan"),
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VsBestSectionMatchedPreview() {
    LeafLogTheme {
        VsBestSection(
            versus = VsBest.Versus(
                previewSession(id = "best", ratings = listOf(4.5f)),
                emptyList()
            ),
            session = previewSession(ratings = listOf(4.5f)),
            vessel = null,
            bestVessel = null,
            modifier = Modifier.padding(24.dp),
        )
    }
}

private fun previewVessel(name: String) = BrewingVessel(
    id = name,
    name = name,
    iconName = "icon",
    createdAt = PreviewInstant,
    updatedAt = PreviewInstant,
    isSystemDefault = false,
    displayOrder = 0,
)

@Preview(showBackground = true)
@Composable
private fun SaveMethodCardPreview() {
    LeafLogTheme {
        SaveMethodCard(
            label = "5g · 100ml · 25s",
            onLabelChange = {},
            onSave = {},
            modifier = Modifier.padding(24.dp)
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
            modifier = Modifier.padding(24.dp),
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
            modifier = Modifier.padding(24.dp)
        )
    }
}
