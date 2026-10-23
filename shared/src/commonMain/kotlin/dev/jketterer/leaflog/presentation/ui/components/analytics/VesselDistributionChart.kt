package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.VesselDistribution
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_vessels
import leaf_log.shared.generated.resources.analytics_vessels_other
import leaf_log.shared.generated.resources.analytics_vessels_summary
import org.jetbrains.compose.resources.stringResource

@Composable
fun VesselDistributionChart(
    distribution: List<VesselDistribution>,
    onTapSegment: (vesselId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    DistributionDonutCard(
        title = stringResource(Res.string.analytics_vessels),
        slices = vesselSlices(
            distribution = distribution,
            palette = listOf(
                colors.primary,
                colors.secondary,
                colors.tertiary,
                colors.primaryContainer,
                colors.secondaryContainer,
            ),
            otherLabel = stringResource(Res.string.analytics_vessels_other),
            otherColor = colors.outline,
        ),
        summary = Res.string.analytics_vessels_summary,
        onTapSlice = onTapSegment,
        modifier = modifier,
    )
}

/**
 * A slice per vessel in [distribution] while [palette] lasts, the rest folded into one untappable
 * [otherLabel] slice.
 */
internal fun vesselSlices(
    distribution: List<VesselDistribution>,
    palette: List<Color>,
    otherLabel: String,
    otherColor: Color,
): List<DonutSlice> {
    val top = distribution.take(palette.size).mapIndexed { index, it ->
        DonutSlice(
            id = it.vessel.id,
            label = it.vessel.name,
            count = it.sessionCount,
            percentage = it.percentage,
            color = palette[index],
        )
    }
    val rest = distribution.drop(palette.size)
    if (rest.isEmpty()) return top
    return top + DonutSlice(
        id = null,
        label = otherLabel,
        count = rest.sumOf { it.sessionCount },
        percentage = rest.sumOf { it.percentage.toDouble() }.toFloat(),
        color = otherColor,
    )
}

@Preview(showBackground = true)
@Composable
private fun VesselDistributionChartPreview() {
    val counts = listOf(
        "Gaiwan" to 18,
        "Teapot" to 12,
        "French Press" to 10,
        "Kyusu" to 6,
        "Mug" to 4,
        "Shiboridashi" to 2,
        "Bowl" to 1,
    )
    val total = counts.sumOf { it.second }
    LeafLogTheme {
        VesselDistributionChart(
            distribution = counts.mapIndexed { index, (name, count) ->
                VesselDistribution(
                    vessel = BrewingVessel(
                        id = "$index",
                        name = name,
                        iconName = null,
                        isSystemDefault = false,
                        displayOrder = index,
                        createdAt = PreviewInstant,
                        updatedAt = PreviewInstant,
                    ),
                    sessionCount = count,
                    percentage = count * 100f / total,
                )
            },
            onTapSegment = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
