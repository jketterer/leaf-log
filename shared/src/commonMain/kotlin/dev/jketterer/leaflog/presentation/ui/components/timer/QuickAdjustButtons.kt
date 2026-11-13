package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.components.common.formatSteepIncrement
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The steps a timer adjusts by, from the steep's planned length: seconds for a short gongfu steep,
 * half-minutes and minutes for a long one. Keyed on the plan, not the time left, so the buttons
 * don't change under a finger mid-steep.
 */
internal fun adjustSteps(planned: Duration): List<Duration> =
    if (planned < 1.minutes) {
        listOf((-10).seconds, (-5).seconds, 5.seconds, 10.seconds)
    } else {
        listOf((-1).minutes, (-30).seconds, 30.seconds, 1.minutes)
    }

/** One row of buttons that add or take time off a running steep, sized to the steep's plan. */
@Composable
fun QuickAdjustButtons(
    planned: Duration,
    onAdjust: (Duration) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // Wraps only at large font scales, where four don't fit a phone's width
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        adjustSteps(planned).forEach { step ->
            OutlinedButton(
                onClick = { onAdjust(step) },
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = formatSteepIncrement(step),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAdjustButtonsGongfuPreview() {
    LeafLogTheme {
        QuickAdjustButtons(planned = 20.seconds, onAdjust = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAdjustButtonsWesternPreview() {
    LeafLogTheme {
        QuickAdjustButtons(planned = 3.minutes, onAdjust = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAdjustButtonsDisabledPreview() {
    LeafLogTheme {
        QuickAdjustButtons(planned = 20.seconds, onAdjust = {}, enabled = false)
    }
}
