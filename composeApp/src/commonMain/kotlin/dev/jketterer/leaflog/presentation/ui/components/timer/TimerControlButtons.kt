package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
fun TimerControlButtons(
    isRunning: Boolean,
    isPaused: Boolean,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onResetClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        when {
            isRunning -> {
                OutlinedButton(onClick = onStopClick) {
                    Text("Stop")
                }
                Button(onClick = onPauseClick) {
                    Text("⏸ Pause")
                }
            }

            isPaused -> {
                OutlinedButton(onClick = onResetClick) {
                    Text("↺ Reset")
                }
                OutlinedButton(onClick = onStopClick) {
                    Text("Stop")
                }
                Button(onClick = onResumeClick) {
                    Text("▶ Resume")
                }
            }

            else -> {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier.fillMaxWidth(0.7f),
                ) {
                    Text("Start Brewing")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerControlButtonsRunningPreview() {
    LeafLogTheme {
        TimerControlButtons(
            isRunning = true,
            isPaused = false,
            onStartClick = {},
            onPauseClick = {},
            onResumeClick = {},
            onResetClick = {},
            onStopClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerControlButtonsPausedPreview() {
    LeafLogTheme {
        TimerControlButtons(
            isRunning = false,
            isPaused = true,
            onStartClick = {},
            onPauseClick = {},
            onResumeClick = {},
            onResetClick = {},
            onStopClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerControlButtonsNotStartedPreview() {
    LeafLogTheme {
        TimerControlButtons(
            isRunning = false,
            isPaused = false,
            onStartClick = {},
            onPauseClick = {},
            onResumeClick = {},
            onResetClick = {},
            onStopClick = {},
        )
    }
}
