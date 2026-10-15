package dev.jketterer.leaflog.presentation.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dev.jketterer.leaflog.domain.models.ThemeMode

// enableEdgeToEdge's own navigation bar scrims, which it doesn't expose
private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@Composable
internal actual fun SystemAppearanceEffect(themeMode: ThemeMode, darkTheme: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity, darkTheme) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) {
                darkTheme
            },
            navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
        )
        onDispose {}
    }
}
