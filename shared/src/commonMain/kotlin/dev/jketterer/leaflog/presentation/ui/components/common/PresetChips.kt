package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

data class Preset<T>(
    val label: String,
    val value: T,
)

@Composable
fun <T> PresetChips(
    presets: List<Preset<T>>,
    currentValue: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: (presetValue: T, currentValue: T?) -> Boolean = { preset, current ->
        preset == current
    },
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(presets) { preset ->
            FilterChip(
                selected = isSelected(preset.value, currentValue),
                onClick = { onSelect(preset.value) },
                label = { Text(preset.label) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PresetChipsPreview() {
    LeafLogTheme {
        PresetChips(
            presets = listOf(
                Preset("100 mL", 100),
                Preset("200 mL", 200),
                Preset("300 mL", 300),
                Preset("500 mL", 500),
            ),
            currentValue = 200,
            onSelect = {},
        )
    }
}
