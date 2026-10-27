package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.already_brewed
import leaf_log.shared.generated.resources.pause
import leaf_log.shared.generated.resources.start_brewing
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimerControlButtons(
    isRunning: Boolean,
    isPaused: Boolean,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Records the steep without timing it; null where there's no steep to record
    onAlreadyBrewedClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        when {
            isRunning -> {
                Button(onClick = onPauseClick) {
                    Text(stringResource(Res.string.pause))
                }
            }

            isPaused -> {
                OutlinedButton(onClick = onResetClick) {
                    Text("Reset")
                }
                Button(onClick = onResumeClick) {
                    Text("Resume")
                }
            }

            // Already brewed is for a steep timed elsewhere, or not at all
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier.fillMaxWidth(0.7f),
                ) {
                    Text(stringResource(Res.string.start_brewing))
                }
                if (onAlreadyBrewedClick != null) {
                    TextButton(onClick = onAlreadyBrewedClick) {
                        Text(stringResource(Res.string.already_brewed))
                    }
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
            onAlreadyBrewedClick = {},
        )
    }
}
