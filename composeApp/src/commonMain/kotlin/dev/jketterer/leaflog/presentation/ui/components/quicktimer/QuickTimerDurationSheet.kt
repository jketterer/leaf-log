package dev.jketterer.leaflog.presentation.ui.components.quicktimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Bottom sheet for selecting quick timer duration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTimerDurationSheet(
    onDismiss: () -> Unit,
    onStartTimer: (durationSeconds: Int) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var duration by remember { mutableStateOf<Duration?>(2.minutes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        QuickTimerDurationSheetContent(
            duration = duration,
            onDurationChange = { duration = it },
            onCancel = onDismiss,
            onStart = {
                duration?.let { d ->
                    onStartTimer(d.inWholeSeconds.toInt())
                }
            },
        )
    }
}

@Composable
private fun QuickTimerDurationSheetContent(
    duration: Duration?,
    onDurationChange: (Duration?) -> Unit,
    onCancel: () -> Unit,
    onStart: () -> Unit,
) {
    val isValid = duration != null && duration > Duration.ZERO

    // Track preset clicks to force DurationPicker sync
    var presetSyncKey by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Quick Timer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start a timer without logging session details",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Duration picker
        DurationPicker(
            duration = duration,
            onDurationChange = onDurationChange,
            label = "Timer Duration",
            isError = duration != null && duration <= Duration.ZERO,
            errorMessage = if (duration != null && duration <= Duration.ZERO) {
                "Duration must be greater than 0"
            } else {
                null
            },
            syncKey = presetSyncKey,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick presets
        Text(
            text = "Quick presets",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PresetButton(
                text = "30s",
                onClick = {
                    onDurationChange(30.seconds)
                    presetSyncKey++
                },
                modifier = Modifier.weight(1f),
            )
            PresetButton(
                text = "1m",
                onClick = {
                    onDurationChange(1.minutes)
                    presetSyncKey++
                },
                modifier = Modifier.weight(1f),
            )
            PresetButton(
                text = "2m",
                onClick = {
                    onDurationChange(2.minutes)
                    presetSyncKey++
                },
                modifier = Modifier.weight(1f),
            )
            PresetButton(
                text = "3m",
                onClick = {
                    onDurationChange(3.minutes)
                    presetSyncKey++
                },
                modifier = Modifier.weight(1f),
            )
            PresetButton(
                text = "5m",
                onClick = {
                    onDurationChange(5.minutes)
                    presetSyncKey++
                },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onStart,
                enabled = isValid,
                modifier = Modifier.weight(1f),
            ) {
                Text("Start Timer")
            }
        }
    }
}

@Composable
private fun PresetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(text)
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickTimerDurationSheetContentPreview() {
    LeafLogTheme {
        QuickTimerDurationSheetContent(
            duration = 2.minutes,
            onDurationChange = {},
            onCancel = {},
            onStart = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickTimerDurationSheetContentEmptyPreview() {
    LeafLogTheme {
        QuickTimerDurationSheetContent(
            duration = null,
            onDurationChange = {},
            onCancel = {},
            onStart = {},
        )
    }
}
