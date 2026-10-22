package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Calendar
import compose.icons.feathericons.Clock
import compose.icons.feathericons.X
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.formatDate
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.cancel
import leaf_log.shared.generated.resources.clear_field
import leaf_log.shared.generated.resources.ok
import leaf_log.shared.generated.resources.select_field
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Read-only date field that opens a Material 3 date picker on tap.
 *
 * The field is not typable: the picker is the only way to set a date, and when [clearable] the
 * trailing icon clears it back to null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    clearable: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }

    PickerTextField(
        value = date?.formatDisplay() ?: "",
        label = label,
        enabled = enabled,
        onOpen = { showPicker = true },
        modifier = modifier,
        trailingIcon = {
            if (date == null || !clearable) {
                Icon(
                    imageVector = FeatherIcons.Calendar,
                    contentDescription = null,
                )
            } else {
                IconButton(onClick = { onDateChange(null) }, enabled = enabled) {
                    Icon(
                        imageVector = FeatherIcons.X,
                        contentDescription = stringResource(Res.string.clear_field, label),
                    )
                }
            }
        },
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.toEpochMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateChange(pickerState.selectedDateMillis?.toLocalDate())
                        showPicker = false
                    },
                ) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Read-only clock time field that opens a Material 3 time picker on tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }

    PickerTextField(
        value = TimeFormatter.formatClockTime(hour, minute),
        label = label,
        enabled = enabled,
        onOpen = { showPicker = true },
        modifier = modifier,
        trailingIcon = { Icon(imageVector = FeatherIcons.Clock, contentDescription = null) },
    )

    if (showPicker) {
        val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute)
        TimePickerDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(pickerState.hour, pickerState.minute)
                        showPicker = false
                    },
                ) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        ) {
            TimePicker(state = pickerState)
        }
    }
}

@Composable
private fun PickerTextField(
    value: String,
    label: String,
    enabled: Boolean,
    onOpen: () -> Unit,
    trailingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A read-only text field still consumes taps for cursor placement, so an outer clickable never
    // fires. Observing the field's own interaction source is the supported way in.
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, enabled) {
        interactionSource.interactions.collect { interaction ->
            if (enabled && interaction is PressInteraction.Release) {
                onOpen()
            }
        }
    }

    val selectLabel = stringResource(Res.string.select_field, label)
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(label) },
        modifier = modifier.semantics {
            role = Role.Button
            onClick(label = selectLabel) {
                onOpen()
                true
            }
        },
        interactionSource = interactionSource,
        trailingIcon = trailingIcon,
    )
}

// The picker speaks epoch millis in UTC; a LocalDate has no time zone, so both directions pin to
// UTC. Converting through the device zone would shift the date across the midnight boundary.
private fun LocalDate.toEpochMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

@Composable
private fun LocalDate.formatDisplay(): String = TimeFormatter.formatDate(this, currentDate())

@Preview(showBackground = true)
@Composable
private fun DatePickerFieldPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            DatePickerField(
                date = LocalDate(2026, 3, 14),
                onDateChange = {},
                label = "Purchased",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DatePickerFieldEmptyPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            DatePickerField(
                date = null,
                onDateChange = {},
                label = "Purchased",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimePickerFieldPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            TimePickerField(
                hour = 14,
                minute = 30,
                onTimeChange = { _, _ -> },
                label = "Time",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
