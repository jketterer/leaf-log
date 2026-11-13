package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.water_type_alkaline
import leaf_log.shared.generated.resources.water_type_distilled
import leaf_log.shared.generated.resources.water_type_filtered
import leaf_log.shared.generated.resources.water_type_mineral
import leaf_log.shared.generated.resources.water_type_other
import leaf_log.shared.generated.resources.water_type_spring
import leaf_log.shared.generated.resources.water_type_tap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Every water type as a chip, picked with one tap and no keyboard in the way. */
@Composable
fun WaterTypeChips(
    selectedWaterType: WaterType,
    onWaterTypeSelected: (WaterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WaterType.entries.forEach { waterType ->
            FilterChip(
                selected = waterType == selectedWaterType,
                onClick = { onWaterTypeSelected(waterType) },
                label = { Text(stringResource(waterType.chipLabel)) },
            )
        }
    }
}

private val WaterType.chipLabel: StringResource
    get() = when (this) {
        WaterType.FILTERED -> Res.string.water_type_filtered
        WaterType.TAP -> Res.string.water_type_tap
        WaterType.SPRING -> Res.string.water_type_spring
        WaterType.DISTILLED -> Res.string.water_type_distilled
        WaterType.MINERAL -> Res.string.water_type_mineral
        WaterType.ALKALINE -> Res.string.water_type_alkaline
        WaterType.OTHER -> Res.string.water_type_other
    }

@Preview(showBackground = true)
@Composable
private fun WaterTypeChipsPreview() {
    LeafLogTheme {
        WaterTypeChips(
            selectedWaterType = WaterType.SPRING,
            onWaterTypeSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
