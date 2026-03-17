package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Volume input field with tappable unit suffix for toggling between mL and fl oz.
 *
 * @param value Current volume value as string
 * @param onValueChange Callback when the text value changes
 * @param currentUnit Current volume unit (Milliliters or Fluid Ounces)
 * @param onToggleUnit Callback when the unit suffix is tapped, receives current value
 * @param label Composable label for the field
 * @param modifier Modifier for the field
 * @param isError Whether the field shows an error state
 * @param supportingText Optional supporting/error text below the field
 * @param enabled Whether the field is enabled for interaction
 */
@Composable
fun VolumeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    currentUnit: VolumeUnit,
    onToggleUnit: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        supportingText = supportingText,
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        suffix = {
            Box(
                modifier = Modifier
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClickLabel = "Toggle volume unit",
                    ) {
                        onToggleUnit()
                    }
                    .padding(4.dp),
            ) {
                Text(
                    text = currentUnit.symbol,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        },
    )
}

@Preview
@Composable
private fun VolumeInputFieldPreview() {
    LeafLogTheme {
        VolumeInputField(
            value = "200",
            onValueChange = {},
            currentUnit = VolumeUnit.MILLILITERS,
            onToggleUnit = {},
            label = { Text("Water Quantity") },
        )
    }
}

@Preview
@Composable
private fun VolumeInputFieldFluidOuncesPreview() {
    LeafLogTheme {
        VolumeInputField(
            value = "7",
            onValueChange = {},
            currentUnit = VolumeUnit.FLUID_OUNCES,
            onToggleUnit = {},
            label = { Text("Water Quantity") },
            isError = false,
        )
    }
}

@Preview
@Composable
private fun VolumeInputFieldErrorPreview() {
    LeafLogTheme {
        VolumeInputField(
            value = "abc",
            onValueChange = {},
            currentUnit = VolumeUnit.MILLILITERS,
            onToggleUnit = {},
            label = { Text("Water Quantity") },
            isError = true,
            supportingText = { Text("Invalid volume") },
        )
    }
}
