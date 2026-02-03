package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.TemperatureUnit

@Composable
fun TempDisplay(
    tempCelsius: Int,
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