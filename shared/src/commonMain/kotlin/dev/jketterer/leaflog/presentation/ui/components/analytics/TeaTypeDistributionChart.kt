package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.isDarkTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_tea_types
import leaf_log.shared.generated.resources.analytics_tea_types_summary
import org.jetbrains.compose.resources.stringResource

@Composable
fun TeaTypeDistributionChart(
    distribution: List<TeaTypeDistribution>,
    onTapSegment: (teaTypeId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isDarkTheme()
    DistributionDonutCard(
        title = stringResource(Res.string.analytics_tea_types),
        slices = distribution.mapIndexed { index, it ->
            DonutSlice(
                id = it.teaType.id,
                label = it.teaType.name,
                count = it.sessionCount,
                percentage = it.percentage,
                color = getChartColor(it.teaType.colorHex, index, dark),
            )
        },
        summary = Res.string.analytics_tea_types_summary,
        onTapSlice = onTapSegment,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun TeaTypeDistributionChartPreview() {
    LeafLogTheme {
        TeaTypeDistributionChart(
            distribution = listOf(
                TeaTypeDistribution(
                    teaType = TeaType(id = "1", name = "Green", colorHex = "#4CAF50"),
                    sessionCount = 15,
                    percentage = 37.5f,
                ),
                TeaTypeDistribution(
                    teaType = TeaType(id = "2", name = "Black", colorHex = "#795548"),
                    sessionCount = 10,
                    percentage = 25f,
                ),
                TeaTypeDistribution(
                    teaType = TeaType(id = "3", name = "Oolong", colorHex = "#FF9800"),
                    sessionCount = 8,
                    percentage = 20f,
                ),
                TeaTypeDistribution(
                    teaType = TeaType(id = "4", name = "White", colorHex = "#FFEB3B"),
                    sessionCount = 4,
                    percentage = 10f,
                ),
                TeaTypeDistribution(
                    teaType = TeaType(id = "5", name = "Pu-erh", colorHex = "#3E2723"),
                    sessionCount = 3,
                    percentage = 7.5f,
                ),
            ),
            onTapSegment = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
