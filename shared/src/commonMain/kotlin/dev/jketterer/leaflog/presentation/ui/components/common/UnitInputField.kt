package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Repeat
import dev.jketterer.leaflog.domain.models.MeasurementUnit
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WeightUnit
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.unit_celsius
import leaf_log.shared.generated.resources.unit_fahrenheit
import leaf_log.shared.generated.resources.unit_fluid_ounces
import leaf_log.shared.generated.resources.unit_grams
import leaf_log.shared.generated.resources.unit_milliliters
import leaf_log.shared.generated.resources.unit_ounces
import leaf_log.shared.generated.resources.unit_switch_to
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A temperature, volume or weight, held in canonical units and typed in [unit] or, after a tap on
 * its unit chip, the other one. The switch lasts until the form closes and never writes Settings.
 *
 * The field owns its text: [onValueChange] fires only when the user types or picks a preset, so
 * switching units alone never changes stored data, and typed text that reads the same as the value
 * the field was given reports that value back exactly.
 */
@Composable
fun UnitInputField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    unit: MeasurementUnit,
    label: String,
    modifier: Modifier = Modifier,
    presets: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    // Whether the user switched away from [unit], rather than the unit itself, so the field still
    // follows preferences that arrive after it first composes.
    var switched by rememberSaveable { mutableStateOf(false) }
    val shown = if (switched) unit.toggle() else unit
    var opened by rememberSaveable { mutableStateOf(value) }
    var text by rememberSaveable { mutableStateOf(unitValueToText(value, shown)) }

    LaunchedEffect(shown) { text = unitValueToText(value, shown) }
    // A value set from outside (a load, a seed) replaces the text; typing that already means it
    // is left alone.
    LaunchedEffect(value) {
        if (unitValueToText(unitTextToValue(text, shown), shown) != unitValueToText(value, shown)) {
            text = unitValueToText(value, shown)
            opened = value
        }
    }

    fun type(input: String) {
        text = input
        onValueChange(typedUnitValue(input, shown, opened))
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                val normalized = input.replace(',', '.')
                if (isUnitInput(normalized, shown)) type(normalized)
            },
            label = { Text(label) },
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (shown.decimals > 0) KeyboardType.Decimal else KeyboardType.Number,
            ),
            enabled = enabled,
            trailingIcon = {
                UnitChip(
                    unit = shown,
                    switched = switched,
                    enabled = enabled,
                    onClick = { switched = !switched },
                )
            },
        )
        if (presets) {
            PresetChips(
                presets = presetsFor(shown).map { preset ->
                    val presetText = formatDecimals(preset, shown.decimals)
                    Preset(withSymbol(presetText, shown), presetText)
                },
                currentValue = text,
                onSelect = ::type,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UnitChip(
    unit: MeasurementUnit,
    switched: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val name = stringResource(unit.spokenName)
    val switchLabel = stringResource(Res.string.unit_switch_to, stringResource(unit.toggle().spokenName))
    // Tertiary marks a unit that differs from Settings.
    val tint = if (switched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    AssistChip(
        onClick = onClick,
        label = { Text(unit.symbol) },
        enabled = enabled,
        trailingIcon = {
            Icon(
                imageVector = FeatherIcons.Repeat,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = tint,
            trailingIconContentColor = tint,
        ),
        border = if (switched) {
            AssistChipDefaults.assistChipBorder(enabled = enabled, borderColor = tint)
        } else {
            AssistChipDefaults.assistChipBorder(enabled = enabled)
        },
        modifier = Modifier
            .padding(end = 8.dp)
            .clearAndSetSemantics {
                contentDescription = name
                role = Role.Button
                if (!enabled) disabled()
                onClick(label = switchLabel) {
                    onClick()
                    true
                }
            },
    )
}

private val MeasurementUnit.spokenName: StringResource
    get() = when (this) {
        is TemperatureUnit -> when (this) {
            TemperatureUnit.CELSIUS -> Res.string.unit_celsius
            TemperatureUnit.FAHRENHEIT -> Res.string.unit_fahrenheit
        }

        is VolumeUnit -> when (this) {
            VolumeUnit.MILLILITERS -> Res.string.unit_milliliters
            VolumeUnit.FLUID_OUNCES -> Res.string.unit_fluid_ounces
        }

        is WeightUnit -> when (this) {
            WeightUnit.GRAMS -> Res.string.unit_grams
            WeightUnit.OUNCES -> Res.string.unit_ounces
        }
    }

/** Common brewing values, in [unit] itself. */
private fun presetsFor(unit: MeasurementUnit): List<Double> = when (unit) {
    is TemperatureUnit -> when (unit) {
        TemperatureUnit.CELSIUS -> listOf(60.0, 70.0, 75.0, 80.0, 85.0, 90.0, 95.0, 100.0)
        TemperatureUnit.FAHRENHEIT -> listOf(140.0, 160.0, 170.0, 175.0, 185.0, 195.0, 200.0, 208.0, 212.0)
    }

    is VolumeUnit -> when (unit) {
        VolumeUnit.MILLILITERS -> listOf(100.0, 150.0, 200.0, 250.0, 300.0, 400.0, 500.0)
        VolumeUnit.FLUID_OUNCES -> listOf(4.0, 6.0, 8.0, 10.0, 12.0, 16.0)
    }

    is WeightUnit -> when (unit) {
        WeightUnit.GRAMS -> listOf(1.0, 2.0, 3.0, 4.0, 5.0, 7.0, 10.0)
        WeightUnit.OUNCES -> listOf(0.05, 0.1, 0.15, 0.2, 0.25, 0.35, 0.5)
    }
}

private fun withSymbol(text: String, unit: MeasurementUnit): String =
    if (unit is TemperatureUnit) "$text${unit.symbol}" else "$text ${unit.symbol}"

/** [value], in canonical units, as the field shows it in [unit]. Null shows as empty. */
internal fun unitValueToText(value: Double?, unit: MeasurementUnit): String =
    value?.let { formatDecimals(unit.fromCanonical(it), unit.decimals) } ?: ""

/** What [text], read in [unit], stands for in canonical units, or null when it holds no number. */
internal fun unitTextToValue(text: String, unit: MeasurementUnit): Double? =
    text.trimEnd('.').toDoubleOrNull()?.let(unit::toCanonical)

/**
 * What typing [text] reports: [opened] when the text reads the same at display precision, so a
 * value retyped as it was shown never drifts (93 °C shows as 199 °F, which is 92.78 °C).
 */
internal fun typedUnitValue(text: String, unit: MeasurementUnit, opened: Double?): Double? {
    val typed = unitTextToValue(text, unit) ?: return null
    val unchanged = opened != null && unitValueToText(typed, unit) == unitValueToText(opened, unit)
    return if (unchanged) opened else typed
}

/** Whether the field may hold [text]: digits, with at most [unit]'s decimals after a point. */
internal fun isUnitInput(text: String, unit: MeasurementUnit): Boolean =
    Regex(if (unit.decimals == 0) "\\d*" else "\\d*(\\.\\d{0,${unit.decimals}})?").matches(text)

/** Rounded to [decimals] places, trailing zeros dropped: "6.8", "7", "0.05". */
private fun formatDecimals(value: Double, decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (value * factor).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val fraction = (abs(scaled) % factor).toString().padStart(decimals, '0').trimEnd('0')
    val whole = "$sign${abs(scaled) / factor}"
    return if (fraction.isEmpty()) whole else "$whole.$fraction"
}

@Preview(showBackground = true)
@Composable
private fun UnitInputFieldPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UnitInputField(
                value = 85.0,
                onValueChange = {},
                unit = TemperatureUnit.CELSIUS,
                label = "Temperature",
                presets = true,
            )
            UnitInputField(
                value = 200.0,
                onValueChange = {},
                unit = VolumeUnit.FLUID_OUNCES,
                label = "Water",
            )
            UnitInputField(
                value = 2.5,
                onValueChange = {},
                unit = WeightUnit.OUNCES,
                label = "Leaf",
                presets = true,
            )
            UnitInputField(
                value = null,
                onValueChange = {},
                unit = VolumeUnit.MILLILITERS,
                label = "Capacity",
                isError = true,
                supportingText = "Capacity must be greater than 0",
            )
            UnitInputField(
                value = 90.0,
                onValueChange = {},
                unit = TemperatureUnit.FAHRENHEIT,
                label = "Temperature",
                enabled = false,
            )
        }
    }
}
