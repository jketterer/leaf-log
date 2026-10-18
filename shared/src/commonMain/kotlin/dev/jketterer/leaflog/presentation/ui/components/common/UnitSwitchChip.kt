package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
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

/**
 * The [unit] a value is shown in, tapped to switch to the other one. [switched] marks a unit that
 * differs from Settings. The caller holds the switch for as long as its screen or form is open;
 * it never writes Settings.
 */
@Composable
fun UnitSwitchChip(
    unit: MeasurementUnit,
    switched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val name = stringResource(unit.spokenName)
    val switchLabel = stringResource(Res.string.unit_switch_to, stringResource(unit.toggle().spokenName))
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
        modifier = modifier.clearAndSetSemantics {
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

/**
 * The switch of [UnitSwitchChip] without the unit, for a tile whose value already shows it. Sits
 * in a corner so it adds no height.
 */
@Composable
fun UnitSwitchIcon(
    unit: MeasurementUnit,
    switched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(unit.spokenName)
    val switchLabel = stringResource(Res.string.unit_switch_to, stringResource(unit.toggle().spokenName))
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (switched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.clearAndSetSemantics {
            contentDescription = name
            role = Role.Button
            onClick(label = switchLabel) {
                onClick()
                true
            }
        },
    ) {
        Icon(
            imageVector = FeatherIcons.Repeat,
            contentDescription = null,
            modifier = Modifier.size(AssistChipDefaults.IconSize),
        )
    }
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

@Preview(showBackground = true)
@Composable
private fun UnitSwitchChipPreview() {
    LeafLogTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UnitSwitchChip(unit = TemperatureUnit.CELSIUS, switched = false, onClick = {})
            UnitSwitchChip(unit = VolumeUnit.FLUID_OUNCES, switched = true, onClick = {})
            UnitSwitchChip(unit = WeightUnit.GRAMS, switched = false, onClick = {}, enabled = false)
            UnitSwitchIcon(unit = VolumeUnit.MILLILITERS, switched = false, onClick = {})
            UnitSwitchIcon(unit = VolumeUnit.FLUID_OUNCES, switched = true, onClick = {})
        }
    }
}
