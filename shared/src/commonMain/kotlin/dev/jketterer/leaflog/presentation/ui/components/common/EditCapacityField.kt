package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
fun EditCapacityField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    supportingText: String,
    enabled: Boolean,
    volumeUnit: VolumeUnit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VolumeInputField(
        value = value,
        onValueChange = onValueChange,
        currentUnit = volumeUnit,
        onToggleUnit = { onToggleUnit() },
        label = { Text("Capacity") },
        isError = isError,
        supportingText = { Text(supportingText) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
    )
}

@Preview(showBackground = true)
@Composable
private fun EditCapacityFieldPreview() {
    var value by remember { mutableStateOf("150") }
    LeafLogTheme {
        EditCapacityField(
            value = value,
            onValueChange = { value = it },
            isError = false,
            supportingText = "Enter vessel capacity",
            enabled = true,
            volumeUnit = VolumeUnit.MILLILITERS,
            onToggleUnit = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditCapacityFieldErrorPreview() {
    LeafLogTheme {
        EditCapacityField(
            value = "",
            onValueChange = {},
            isError = true,
            supportingText = "Capacity is required",
            enabled = true,
            volumeUnit = VolumeUnit.FLUID_OUNCES,
            onToggleUnit = {},
        )
    }
}