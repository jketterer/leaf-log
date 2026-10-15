package dev.jketterer.leaflog.presentation.ui.theme

import androidx.compose.runtime.Composable
import dev.jketterer.leaflog.domain.models.ThemeMode

/** Points the system bars, and what the platform draws itself, at the app's theme. */
@Composable
internal expect fun SystemAppearanceEffect(themeMode: ThemeMode, darkTheme: Boolean)
