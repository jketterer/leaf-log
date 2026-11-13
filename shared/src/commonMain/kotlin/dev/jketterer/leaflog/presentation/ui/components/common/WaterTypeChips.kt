package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_water_type
import org.jetbrains.compose.resources.stringResource

/**
 * Every water type as a chip, picked with one tap and no keyboard in the way. [label] names the
 * chips where nothing else around them does.
 */
@Composable
fun WaterTypeChips(
    selectedWaterType: WaterType,
    onWaterTypeSelected: (WaterType) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WaterType.entries.forEach { waterType ->
                FilterChip(
                    selected = waterType == selectedWaterType,
                    onClick = { onWaterTypeSelected(waterType) },
                    label = { Text(stringResource(waterType.shortLabel)) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WaterTypeChipsPreview() {
    LeafLogTheme {
        WaterTypeChips(
            selectedWaterType = WaterType.SPRING,
            onWaterTypeSelected = {},
            modifier = Modifier.padding(16.dp),
            label = stringResource(Res.string.field_water_type),
        )
    }
}
