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
 * Parses the two text fields into a duration.
 *
 * An empty field counts as zero, seconds are clamped to a single minute, and anything that isn't a
 * number — or two empty fields — yields null.
 */
internal fun parseDurationFields(minutesText: String, secondsText: String): Duration? {
    if (minutesText.isEmpty() && secondsText.isEmpty()) return null
    val minutes = if (minutesText.isEmpty()) 0 else minutesText.toIntOrNull() ?: return null
    val seconds = if (secondsText.isEmpty()) {
        0
    } else {
        secondsText.toIntOrNull()?.coerceIn(0, 59) ?: return null
    }
    return minutes.minutes + seconds.seconds
}

/** Splits a duration into the minutes and seconds text the fields display. */
internal fun durationToFields(duration: Duration): Pair<String, String> =
    duration.inWholeMinutes.toString() to (duration.inWholeSeconds % 60).toString()

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
) {
    // Field text, not the duration itself: "" and "0" are different to a typist but not to a
    // Duration, so the text has to survive a round trip the duration cannot represent.
    var minutesFieldValue by remember {
        mutableStateOf(TextFieldValue(duration?.inWholeMinutes?.toString() ?: ""))
    }
    var secondsFieldValue by remember {
        mutableStateOf(TextFieldValue((duration?.inWholeSeconds?.rem(60))?.toString() ?: ""))
    }

    // Re-seed only when the incoming duration disagrees with what the fields already say, so an
    // async load or a preset lands but ordinary typing is left alone.
    LaunchedEffect(duration) {
        if (duration != null &&
            parseDurationFields(minutesFieldValue.text, secondsFieldValue.text) != duration
        ) {
            val (minutesText, secondsText) = durationToFields(duration)
            minutesFieldValue = TextFieldValue(minutesText)
            secondsFieldValue = TextFieldValue(secondsText)
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
                    onDurationChange(
                        parseDurationFields(newValue.text, secondsFieldValue.text),
                    )
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
                    onDurationChange(
                        parseDurationFields(minutesFieldValue.text, newValue.text),
                    )
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