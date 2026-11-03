package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Clears focus, closing the keyboard, on a tap nothing inside takes. The iOS number pad has no
 * return key, so this is how its fields let go. A `ModalBottomSheet` opens in its own window, out
 * of reach of the app's, so a sheet with fields needs its own.
 */
@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return pointerInput(focusManager) { detectTapGestures { focusManager.clearFocus() } }
}
