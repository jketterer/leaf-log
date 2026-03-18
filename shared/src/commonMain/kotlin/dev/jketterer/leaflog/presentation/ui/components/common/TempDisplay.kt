package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
fun TempDisplay(
    tempCelsius: Double,
    tempUnit: TemperatureUnit,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    Text(
        TemperatureFormatter.format(tempCelsius, tempUnit),
        modifier = modifier,
        style = style,
        color = color,
    )
}

@Preview(showBackground = true)
@Composable
private fun TempDisplayCelsiusPreview() {
    LeafLogTheme {
        TempDisplay(tempCelsius = 95.0, tempUnit = TemperatureUnit.CELSIUS)
    }
}

@Preview(showBackground = true)
@Composable
private fun TempDisplayFahrenheitPreview() {
    LeafLogTheme {
        TempDisplay(tempCelsius = 95.0, tempUnit = TemperatureUnit.FAHRENHEIT)
    }
}