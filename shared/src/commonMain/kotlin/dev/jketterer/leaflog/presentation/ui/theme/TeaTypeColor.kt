package dev.jketterer.leaflog.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import dev.jketterer.leaflog.presentation.ui.components.analytics.hexToColor

/** Oklab lightness that keeps any hue at 3:1 against both themes' surfaces. */
private const val TONE_BOUND = 0.62f

/**
 * A tea type's user-picked [hex], with its lightness clamped so it stands out from the surface:
 * cream darkens on light, dark brown lightens on dark. Hue is kept, and stored data never changes.
 * Unspecified when [hex] isn't a color.
 */
fun teaTypeColor(hex: String, dark: Boolean): Color {
    val color = hex.hexToColor()
    if (color == Color.Unspecified) return color
    val lab = color.convert(ColorSpaces.Oklab)
    val lightness =
        if (dark) lab.red.coerceAtLeast(TONE_BOUND) else lab.red.coerceAtMost(TONE_BOUND)
    return Color(
        lightness,
        lab.green,
        lab.blue,
        colorSpace = ColorSpaces.Oklab
    ).convert(ColorSpaces.Srgb)
}

/** [teaTypeColor] for the theme in effect. */
@Composable
@ReadOnlyComposable
fun teaTypeColor(hex: String): Color = teaTypeColor(hex, isDarkTheme())

/** Whether the theme in effect is dark, which isn't always the system's setting. */
@Composable
@ReadOnlyComposable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** Black or white, whichever reads better on [background]. */
fun contentColorOn(background: Color): Color =
    // The luminance where black and white text have equal contrast
    if (background.luminance() > 0.179f) Color.Black else Color.White
