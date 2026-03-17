package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.ui.graphics.Color

fun String.hexToColor(): Color {
    val hex = this.removePrefix("#")
    if (hex.length != 6) return Color.Unspecified
    return try {
        Color(
            red = hex.substring(0, 2).toInt(16) / 255f,
            green = hex.substring(2, 4).toInt(16) / 255f,
            blue = hex.substring(4, 6).toInt(16) / 255f,
        )
    } catch (_: Exception) {
        Color.Unspecified
    }
}

val chartColors = listOf(
    Color(0xFF4CAF50), // Green
    Color(0xFF2196F3), // Blue
    Color(0xFFFF9800), // Orange
    Color(0xFF9C27B0), // Purple
    Color(0xFFF44336), // Red
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF3F51B5), // Indigo
    Color(0xFFCDDC39), // Lime
    Color(0xFFE91E63), // Pink
)

fun getChartColor(colorHex: String, index: Int): Color {
    val parsed = colorHex.hexToColor()
    return if (parsed == Color.Unspecified) {
        chartColors[index % chartColors.size]
    } else {
        parsed
    }
}
