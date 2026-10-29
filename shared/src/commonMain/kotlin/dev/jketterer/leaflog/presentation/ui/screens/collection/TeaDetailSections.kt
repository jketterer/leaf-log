package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.FlavorProfile
import dev.jketterer.leaflog.domain.models.SteepCurve
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.formatRating
import dev.jketterer.leaflog.presentation.ui.components.session.FlavorRow
import dev.jketterer.leaflog.presentation.ui.components.session.label
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.header_flavor_profile
import leaf_log.shared.generated.resources.header_steep_curve
import leaf_log.shared.generated.resources.steep_curve_bar
import leaf_log.shared.generated.resources.steep_curve_peak
import org.jetbrains.compose.resources.stringResource

/** Each of the tea's top tags with how many of its tagged sessions confirmed it; tapping one opens it. */
@Composable
internal fun FlavorProfileSection(
    profile: FlavorProfile,
    onTagClick: (tagId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(Res.string.header_flavor_profile))
        profile.tags.forEach { (tag, count) ->
            FlavorRow(
                label = tag.label(),
                count = count,
                outOf = profile.sessions,
                isCustom = tag.isCustom,
                onClick = { onTagClick(tag.id) },
            )
        }
    }
}

/** Mean rating by steep as bars out of 5, the peak labeled with its value. */
@Composable
internal fun SteepCurveSection(
    curve: SteepCurve,
    modifier: Modifier = Modifier,
) {
    val barShape = MaterialTheme.shapes.extraSmall.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(Res.string.header_steep_curve))
        Text(stringResource(Res.string.steep_curve_peak, curve.peak), style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            curve.meanRatings.forEach { (steep, rating) ->
                val isPeak = steep == curve.peak
                val description = stringResource(Res.string.steep_curve_bar, steep, formatRating(rating))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Every bar keeps the label's line, so all of them share one scale
                    Text(
                        text = if (isPeak) formatRating(rating) else "",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .widthIn(max = 32.dp)
                                .fillMaxWidth()
                                .fillMaxHeight(rating / 5f)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (isPeak) 1f else 0.45f),
                                    barShape,
                                ),
                        )
                    }
                    Text(
                        text = "$steep",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isPeak) FontWeight.Bold else null,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FlavorProfileSectionPreview() {
    LeafLogTheme {
        FlavorProfileSection(
            onTagClick = {},
            profile = FlavorProfile(
                tags = listOf("floral" to 7, "returning_sweetness" to 5, "smooth" to 3, "astringent" to 1).map { (key, count) ->
                    TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = CURATED_TAG_KEYS.indexOf(key)) to count
                },
                sessions = 12,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepCurveSectionPreview() {
    LeafLogTheme {
        SteepCurveSection(
            curve = SteepCurve(
                meanRatings = mapOf(1 to 3.5f, 2 to 4f, 3 to 4.25f, 4 to 4.75f, 5 to 4.5f, 6 to 3.75f, 8 to 3f),
                peak = 4,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
