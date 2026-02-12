package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
fun PeriodSelector(
    currentLabel: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        FilterChip(
            selected = true,
            onClick = { onExpandChange(!expanded) },
            label = { Text(currentLabel) },
            trailingIcon = {
                Icon(
                    imageVector = FeatherIcons.ChevronDown,
                    contentDescription = "Select period",
                    modifier = Modifier.padding(start = 4.dp),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
        ) {
            AnalyticsPeriod.entries
                .filter { it != AnalyticsPeriod.CUSTOM }
                .forEach { period ->
                    DropdownMenuItem(
                        text = { Text(period.label) },
                        onClick = { onPeriodSelected(period) },
                    )
                }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PeriodSelectorPreview() {
    LeafLogTheme {
        PeriodSelector(
            currentLabel = "This Month",
            expanded = false,
            onExpandChange = {},
            onPeriodSelected = {},
        )
    }
}
