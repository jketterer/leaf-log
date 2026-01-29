package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration

/**
 * Quick adjustment buttons for timer (+/- 30s, +/- 1m).
 */
@Composable
fun QuickAdjustButtons(
    onAdjust: (Duration) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onAdjust(Duration.parse("-1m")) },
                enabled = enabled,
            ) {
                Text("-1m")
            }
            OutlinedButton(
                onClick = { onAdjust(Duration.parse("-30s")) },
                enabled = enabled,
            ) {
                Text("-30s")
            }
            OutlinedButton(
                onClick = { onAdjust(Duration.parse("30s")) },
                enabled = enabled,
            ) {
                Text("+30s")
            }
            OutlinedButton(
                onClick = { onAdjust(Duration.parse("1m")) },
                enabled = enabled,
            ) {
                Text("+1m")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAdjustButtonsPreview() {
    LeafLogTheme {
        QuickAdjustButtons(
            onAdjust = {},
            enabled = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAdjustButtonsDisabledPreview() {
    LeafLogTheme {
        QuickAdjustButtons(
            onAdjust = {},
            enabled = false,
        )
    }
}
