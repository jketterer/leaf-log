package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable

/**
 * A [androidx.compose.material3.ModalBottomSheet] state that opens fully expanded and never stops
 * halfway. A partly expanded sheet holds at half the screen while its content grows, so the
 * keyboard, or content that changes height, pushes what's at the bottom out of sight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberExpandedSheetState(): SheetState = rememberBottomSheetState(
    initialValue = SheetValue.Hidden,
    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
)
