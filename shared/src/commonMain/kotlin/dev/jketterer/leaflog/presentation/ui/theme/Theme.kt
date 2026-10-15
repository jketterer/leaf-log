package dev.jketterer.leaflog.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun LeafLogTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        // Shapes are M3's default scale, 4/8/12/16/28dp; use MaterialTheme.shapes, not a literal
        typography = leafLogTypography(),
        content = content,
    )
}
