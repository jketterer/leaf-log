package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Duration picker for brewing time (minutes and seconds).
 */
@Composable
fun DurationPicker(
    duration: Duration?,
    onDurationChange: (Duration?) -> Unit,
    label: String = "Brewing Time",
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    syncKey: Any? = null,
) {
    // Track whether we've synced from the initial duration prop
    // This prevents re-syncing after user edits while allowing initial async load
    var initialDurationSynced by remember { mutableStateOf(duration != null) }

    // Initialize field values from duration only on first composition
    // After that, fields maintain their own independent state
    var minutesFieldValue by remember {
        mutableStateOf(TextFieldValue(duration?.inWholeMinutes?.toString() ?: ""))
    }
    var secondsFieldValue by remember {
        mutableStateOf(TextFieldValue((duration?.inWholeSeconds?.rem(60))?.toString() ?: ""))
    }

    // Sync from prop when duration changes externally (async load or slider)
    LaunchedEffect(duration) {
        if (duration != null) {
            val currentMin = if (minutesFieldValue.text.isEmpty()) 0
                else minutesFieldValue.text.toIntOrNull()
            val currentSec = if (secondsFieldValue.text.isEmpty()) 0
                else secondsFieldValue.text.toIntOrNull()?.coerceIn(0, 59)
            val currentDuration = if (currentMin != null && currentSec != null) {
                currentMin.minutes + currentSec.seconds
            } else null
            if (currentDuration != duration) {
                minutesFieldValue = TextFieldValue(duration.inWholeMinutes.toString())
                secondsFieldValue = TextFieldValue((duration.inWholeSeconds % 60).toString())
            }
            initialDurationSynced = true
        }
    }

    // Force sync when syncKey changes (e.g., preset button clicked)
    LaunchedEffect(syncKey) {
        if (syncKey != null && duration != null) {
            minutesFieldValue = TextFieldValue(duration.inWholeMinutes.toString())
            secondsFieldValue = TextFieldValue((duration.inWholeSeconds % 60).toString())
        }
    }

    val minutesInteractionSource = remember { MutableInteractionSource() }
    val secondsInteractionSource = remember { MutableInteractionSource() }

    val isMinutesFocused by minutesInteractionSource.collectIsFocusedAsState()
    val isSecondsFocused by secondsInteractionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

    // Select all text when field gains focus
    LaunchedEffect(isMinutesFocused) {
        if (isMinutesFocused && minutesFieldValue.text.isNotEmpty()) {
            minutesFieldValue = minutesFieldValue.copy(
                selection = TextRange(0, minutesFieldValue.text.length)
            )
        }
    }

    LaunchedEffect(isSecondsFocused) {
        if (isSecondsFocused && secondsFieldValue.text.isNotEmpty()) {
            secondsFieldValue = secondsFieldValue.copy(
                selection = TextRange(0, secondsFieldValue.text.length)
            )
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Minutes
            OutlinedTextField(
                value = minutesFieldValue,
                onValueChange = { newValue ->
                    minutesFieldValue = newValue
                    val minText = newValue.text
                    val secText = secondsFieldValue.text

                    // If both fields are empty, return null
                    if (minText.isEmpty() && secText.isEmpty()) {
                        onDurationChange(null)
                    } else {
                        // Treat empty as 0, but validate non-empty values
                        val min = if (minText.isEmpty()) 0 else minText.toIntOrNull()
                        val sec = if (secText.isEmpty()) 0 else secText.toIntOrNull()?.coerceIn(0, 59)

                        if (min != null && sec != null) {
                            onDurationChange(min.minutes + sec.seconds)
                        } else {
                            onDurationChange(null)
                        }
                    }
                },
                label = { Text("Min") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                isError = isError || (minutesFieldValue.text.isNotEmpty() && minutesFieldValue.text.toIntOrNull() == null),
                interactionSource = minutesInteractionSource,
            )

            // Seconds
            OutlinedTextField(
                value = secondsFieldValue,
                onValueChange = { newValue ->
                    secondsFieldValue = newValue
                    val minText = minutesFieldValue.text
                    val secText = newValue.text

                    // If both fields are empty, return null
                    if (minText.isEmpty() && secText.isEmpty()) {
                        onDurationChange(null)
                    } else {
                        // Treat empty as 0, but validate non-empty values
                        val min = if (minText.isEmpty()) 0 else minText.toIntOrNull()
                        val sec = if (secText.isEmpty()) 0 else secText.toIntOrNull()?.coerceIn(0, 59)

                        if (min != null && sec != null) {
                            onDurationChange(min.minutes + sec.seconds)
                        } else {
                            onDurationChange(null)
                        }
                    }
                },
                label = { Text("Sec") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                isError = isError || (secondsFieldValue.text.isNotEmpty() && secondsFieldValue.text.toIntOrNull() == null),
                interactionSource = secondsInteractionSource,
            )
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DurationPickerPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // With value
            DurationPicker(
                duration = 2.minutes + 30.seconds,
                onDurationChange = {},
            )

            // Empty
            DurationPicker(
                duration = null,
                onDurationChange = {},
            )

            // With error
            DurationPicker(
                duration = Duration.ZERO,
                onDurationChange = {},
                isError = true,
                errorMessage = "Brewing time must be greater than 0",
            )
        }
    }
}