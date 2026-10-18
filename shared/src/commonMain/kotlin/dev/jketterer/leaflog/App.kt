package dev.jketterer.leaflog

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import dev.jketterer.leaflog.domain.models.ThemeMode
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.presentation.ui.components.common.LocalUnits
import dev.jketterer.leaflog.presentation.ui.components.common.Units
import dev.jketterer.leaflog.presentation.ui.navigation.AppNavigation
import dev.jketterer.leaflog.presentation.ui.theme.DarkColors
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.LightColors
import dev.jketterer.leaflog.presentation.ui.theme.SystemAppearanceEffect
import org.koin.compose.koinInject

@Composable
fun App() {
    val preferencesRepository: PreferencesRepository = koinInject()
    val preferences by remember { preferencesRepository.getPreferencesFlow() }
        .collectAsState(initial = null)
    val systemDark = isSystemInDarkTheme()

    // Until the setting is read, a plain background in the system's theme, like the splash
    val prefs = preferences ?: run {
        val background = (if (systemDark) DarkColors else LightColors).background
        Box(modifier = Modifier.fillMaxSize().background(background))
        return
    }
    val mode = prefs.themeMode
    val darkTheme = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    SystemAppearanceEffect(themeMode = mode, darkTheme = darkTheme)

    LeafLogTheme(useDarkTheme = darkTheme) {
        CompositionLocalProvider(LocalUnits provides Units(prefs)) {
            val focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { focusManager.clearFocus() }
                    },
            ) {
                AppNavigation()
            }
        }
    }
}
