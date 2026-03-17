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
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Temperature input field with tappable unit suffix for toggling between Celsius and Fahrenheit.
 *
 * @param value Current temperature value as string
 * @param onValueChange Callback when the text value changes
 * @param currentUnit Current temperature unit (Celsius or Fahrenheit)
 * @param onToggleUnit Callback when the unit suffix is tapped, receives current value
 * @param label Composable label for the field
 * @param modifier Modifier for the field
 * @param isError Whether the field shows an error state
 * @param supportingText Optional supporting/error text below the field
 * @param enabled Whether the field is enabled for interaction
 */
@Composable
fun TemperatureInputField(
    value: String,
    onValueChange: (String) -> Unit,
    currentUnit: TemperatureUnit,
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
                        onClickLabel = "Toggle temperature unit",
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
private fun TemperatureInputFieldPreview() {
    LeafLogTheme {
        TemperatureInputField(
            value = "80",
            onValueChange = {},
            currentUnit = TemperatureUnit.CELSIUS,
            onToggleUnit = {},
            label = { Text("Temperature") },
        )
    }
}

@Preview
@Composable
private fun TemperatureInputFieldFahrenheitPreview() {
    LeafLogTheme {
        TemperatureInputField(
            value = "176",
            onValueChange = {},
            currentUnit = TemperatureUnit.FAHRENHEIT,
            onToggleUnit = {},
            label = { Text("Temperature") },
            isError = false,
        )
    }
}

@Preview
@Composable
private fun TemperatureInputFieldErrorPreview() {
    LeafLogTheme {
        TemperatureInputField(
            value = "abc",
            onValueChange = {},
            currentUnit = TemperatureUnit.CELSIUS,
            onToggleUnit = {},
            label = { Text("Temperature") },
            isError = true,
            supportingText = { Text("Invalid temperature") },
        )
    }
}
