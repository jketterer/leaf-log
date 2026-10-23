package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.VesselDistribution
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_vessels
import leaf_log.shared.generated.resources.analytics_vessels_summary
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
fun VesselDistributionChart(
    distribution: List<VesselDistribution>,
    onTapSegment: (vesselId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DistributionDonutCard(
        title = stringResource(Res.string.analytics_vessels),
        slices = distribution.mapIndexed { index, it ->
            DonutSlice(
                id = it.vessel.id,
                label = it.vessel.name,
                count = it.sessionCount,
                percentage = it.percentage,
                color = chartColors[index % chartColors.size],
            )
        },
        summary = Res.string.analytics_vessels_summary,
        onTapSlice = onTapSegment,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun VesselDistributionChartPreview() {
    val now = Instant.fromEpochMilliseconds(0)
    LeafLogTheme {
        VesselDistributionChart(
            distribution = listOf(
                VesselDistribution(
                    vessel = BrewingVessel(
                        id = "1",
                        name = "Gaiwan",
                        iconName = null,
                        isSystemDefault = false,
                        displayOrder = 0,
                        createdAt = now,
                        updatedAt = now
                    ),
                    sessionCount = 18,
                    percentage = 45f,
                ),
                VesselDistribution(
                    vessel = BrewingVessel(
                        id = "2",
                        name = "Teapot",
                        iconName = null,
                        isSystemDefault = false,
                        displayOrder = 1,
                        createdAt = now,
                        updatedAt = now
                    ),
                    sessionCount = 12,
                    percentage = 30f,
                ),
                VesselDistribution(
                    vessel = BrewingVessel(
                        id = "3",
                        name = "French Press",
                        iconName = null,
                        isSystemDefault = false,
                        displayOrder = 2,
                        createdAt = now,
                        updatedAt = now
                    ),
                    sessionCount = 10,
                    percentage = 25f,
                ),
            ),
            onTapSegment = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
