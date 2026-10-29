package dev.jketterer.leaflog.presentation.ui.components.common

import org.jetbrains.compose.resources.stringResource
import leaf_log.shared.generated.resources.field_water_type
import leaf_log.shared.generated.resources.Res
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Dropdown selector for water type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterTypeSelector(
    modifier: Modifier = Modifier,
    selectedWaterType: WaterType,
    onWaterTypeSelected: (WaterType) -> Unit,
    label: String = stringResource(Res.string.field_water_type),
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedWaterType.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                    enabled = true,
                ),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            WaterType.entries.forEach { waterType ->
                DropdownMenuItem(
                    text = { Text(waterType.displayName) },
                    onClick = {
                        onWaterTypeSelected(waterType)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WaterTypeSelectorPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            WaterTypeSelector(
                selectedWaterType = WaterType.FILTERED,
                onWaterTypeSelected = {},
            )
        }
    }
}