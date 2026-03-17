package dev.jketterer.leaflog

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.presentation.ui.navigation.AppNavigation
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
@Preview
fun App() {
    LeafLogTheme {
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