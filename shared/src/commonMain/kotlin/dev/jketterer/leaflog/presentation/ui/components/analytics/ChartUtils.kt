package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

/**
 * Index of the donut segment under a tap at ([dx], [dy]) from the center, or null when the tap
 * misses the ring. Segments run clockwise from 12 o'clock with the given [sweeps] in degrees.
 */
internal fun donutSegmentAt(
    dx: Float,
    dy: Float,
    innerRadius: Float,
    outerRadius: Float,
    sweeps: List<Float>,
): Int? {
    if (sweeps.isEmpty() || sqrt(dx * dx + dy * dy) !in innerRadius..outerRadius) return null

    // atan2 measures from 3 o'clock; screen y points down, so it already runs clockwise
    val angleFromTop = (atan2(dy, dx) * 180f / PI.toFloat() + 90f + 360f) % 360f
    var segmentEnd = 0f
    sweeps.forEachIndexed { index, sweep ->
        segmentEnd += sweep
        if (angleFromTop < segmentEnd) return index
    }
    return sweeps.lastIndex // float rounding can leave the sweeps a hair short of 360
}

/** Index of the evenly spaced point nearest [x], the first point sitting at [firstX]. */
internal fun nearestPointIndex(x: Float, firstX: Float, stepX: Float, count: Int): Int =
    ((x - firstX) / stepX).roundToInt().coerceIn(0, count - 1)
