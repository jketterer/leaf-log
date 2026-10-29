package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/** A tag to tap, outlined in the tertiary that tags read in; [isCustom] marks one the user made. */
@Composable
fun FlavorChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCustom: Boolean = false,
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label) },
        icon = if (isCustom) ({ YourTagIcon() }) else null,
        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = MaterialTheme.colorScheme.tertiary),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun FlavorChipPreview() {
    LeafLogTheme {
        FlowRow(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlavorChip(label = "Umami", onClick = {})
            FlavorChip(label = "Petrichor", onClick = {}, isCustom = true)
        }
    }
}
