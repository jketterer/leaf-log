package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.WeightUnit
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_tea_quantity
import leaf_log.shared.generated.resources.loose_leaf
import leaf_log.shared.generated.resources.review_tea_bag
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Loose leaf / tea bag toggle, with the leaf weight input for loose leaf brews. */
@Composable
fun LeafQuantityField(
    isTeaBag: Boolean,
    teaQuantityGrams: Double?,
    onTeaBagModeChanged: (Boolean) -> Unit,
    onTeaQuantityChanged: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.field_tea_quantity),
    errorMessage: StringResource? = null,
    presets: Boolean = false,
    weightUnit: WeightUnit = LocalUnits.current.weight,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { onTeaBagModeChanged(false) },
                selected = !isTeaBag,
                label = { Text(stringResource(Res.string.loose_leaf)) },
                icon = {},
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { onTeaBagModeChanged(true) },
                selected = isTeaBag,
                label = { Text(stringResource(Res.string.review_tea_bag)) },
                icon = {},
            )
        }
        if (!isTeaBag) {
            UnitInputField(
                value = teaQuantityGrams,
                onValueChange = onTeaQuantityChanged,
                unit = weightUnit,
                label = label,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { stringResource(it) },
                presets = presets,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LeafQuantityFieldPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LeafQuantityField(
                isTeaBag = false,
                teaQuantityGrams = 5.0,
                onTeaBagModeChanged = {},
                onTeaQuantityChanged = {},
                presets = true,
            )
            LeafQuantityField(
                isTeaBag = true,
                teaQuantityGrams = null,
                onTeaBagModeChanged = {},
                onTeaQuantityChanged = {},
            )
        }
    }
}
