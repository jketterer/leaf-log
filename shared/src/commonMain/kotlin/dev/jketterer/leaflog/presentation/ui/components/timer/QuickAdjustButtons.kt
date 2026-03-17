package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
    val compactPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Fine adjustments — closer to the timer ring
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onAdjust(Duration.parse("-10s")) },
                enabled = enabled,
                contentPadding = compactPadding,
            ) {
                Text("-10s", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = { onAdjust(Duration.parse("-5s")) },
                enabled = enabled,
                contentPadding = compactPadding,
            ) {
                Text("-5s", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = { onAdjust(Duration.parse("5s")) },
                enabled = enabled,
                contentPadding = compactPadding,
            ) {
                Text("+5s", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = { onAdjust(Duration.parse("10s")) },
                enabled = enabled,
                contentPadding = compactPadding,
            ) {
                Text("+10s", style = MaterialTheme.typography.labelMedium)
            }
        }
        // Coarse adjustments
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = { onAdjust(Duration.parse("-1m")) },
                enabled = enabled,
                contentPadding = compactPadding,
            ) {
                Text("-1m", style = MaterialTheme.typography.labelLarge)
            }
            FilledTonalButton(
                onClick = { onAdjust(Duration.parse("-30s")) },
                enabled = enabled,
                contentPadding = compactPadding,
            ) {
                Text("-30s", style = MaterialTheme.typography.labelLarge)
            }
            FilledTonalButton(
                onClick = { onAdjust(Duration.parse("30s")) },
                enabled = enabled,
                contentPadding = compactPadding,
            ) {
                Text("+30s", style = MaterialTheme.typography.labelLarge)
            }
            FilledTonalButton(
                onClick = { onAdjust(Duration.parse("1m")) },
                enabled = enabled,
                contentPadding = compactPadding,
            ) {
                Text("+1m", style = MaterialTheme.typography.labelLarge)
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
