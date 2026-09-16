package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Lays sheet content out the way a ModalBottomSheet would, for use in @Preview functions.
 *
 * Previews host a real sheet in a separate window that the renderer does not capture, so a preview
 * of a ModalBottomSheet comes out blank. Previewing the sheet's body inside this container renders
 * it, at [height] instead of the screen height the real sheet would take.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetPreviewContainer(
    modifier: Modifier = Modifier,
    height: Dp = 600.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = BottomSheetDefaults.ExpandedShape,
        color = BottomSheetDefaults.ContainerColor,
    ) {
        Column {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SheetPreviewContainerPreview() {
    LeafLogTheme {
        SheetPreviewContainer(height = 200.dp) {
            Text("Sheet body content")
        }
    }
}
