package dev.jketterer.leaflog.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.jketterer.leaflog.domain.models.ThemeMode
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * Overrides the windows' style, which the status bar and native pickers follow. System clears the
 * override; setting light or dark for it would stop isSystemInDarkTheme seeing the real setting.
 */
@Composable
internal actual fun SystemAppearanceEffect(themeMode: ThemeMode, darkTheme: Boolean) {
    LaunchedEffect(themeMode) {
        val style = when (themeMode) {
            ThemeMode.SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            ThemeMode.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            ThemeMode.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { it.windows.filterIsInstance<UIWindow>() }
            .forEach { it.overrideUserInterfaceStyle = style }
    }
}
