package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_period_all_time
import leaf_log.shared.generated.resources.analytics_period_this_month
import leaf_log.shared.generated.resources.analytics_period_this_season
import leaf_log.shared.generated.resources.analytics_period_this_week
import leaf_log.shared.generated.resources.analytics_period_this_year
import leaf_log.shared.generated.resources.analytics_select_period
import org.jetbrains.compose.resources.stringResource

@Composable
fun PeriodSelector(
    selected: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilterChip(
            selected = true,
            onClick = { expanded = !expanded },
            label = { Text(periodLabel(selected)) },
            trailingIcon = {
                Icon(
                    imageVector = FeatherIcons.ChevronDown,
                    contentDescription = stringResource(Res.string.analytics_select_period),
                    modifier = Modifier.padding(start = 4.dp),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AnalyticsPeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = { Text(periodLabel(period)) },
                    onClick = {
                        expanded = false
                        onPeriodSelected(period)
                    },
                )
            }
        }
    }
}

@Composable
private fun periodLabel(period: AnalyticsPeriod): String = stringResource(
    when (period) {
        AnalyticsPeriod.THIS_WEEK -> Res.string.analytics_period_this_week
        AnalyticsPeriod.THIS_MONTH -> Res.string.analytics_period_this_month
        AnalyticsPeriod.THIS_SEASON -> Res.string.analytics_period_this_season
        AnalyticsPeriod.THIS_YEAR -> Res.string.analytics_period_this_year
        AnalyticsPeriod.ALL_TIME -> Res.string.analytics_period_all_time
    },
)

@Preview(showBackground = true)
@Composable
private fun PeriodSelectorPreview() {
    LeafLogTheme {
        PeriodSelector(
            selected = AnalyticsPeriod.THIS_SEASON,
            onPeriodSelected = {},
        )
    }
}
