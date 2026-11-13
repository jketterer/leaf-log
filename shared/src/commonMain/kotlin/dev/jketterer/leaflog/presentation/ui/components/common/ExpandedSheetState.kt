package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * A [androidx.compose.material3.ModalBottomSheet] state that opens fully expanded and never stops
 * halfway. A partly expanded sheet holds at half the screen while its content grows, so the
 * keyboard, or content that changes height, pushes what's at the bottom out of sight.
 *
 * A swipe, a tap outside or back closes the sheet only when [confirmHide] agrees; refused, the
 * sheet settles back open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberExpandedSheetState(confirmHide: () -> Boolean = { true }): SheetState {
    val currentConfirmHide by rememberUpdatedState(confirmHide)
    return rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        // Remembered, since the state is rebuilt whenever this lambda changes
        confirmValueChange = remember { { value -> value != SheetValue.Hidden || currentConfirmHide() } },
    )
}
