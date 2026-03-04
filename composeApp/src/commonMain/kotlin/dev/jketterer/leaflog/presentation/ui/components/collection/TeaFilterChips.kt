package dev.jketterer.leaflog.presentation.ui.components.collection

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Heart
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.screens.collection.FilterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Instant

@Composable
fun TeaFilterChips(
    teaTypes: List<TeaType>,
    selectedFilter: FilterType,
    selectedTypeId: String?,
    onFilterSelected: (FilterType, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All filter
        FilterChip(
            selected = selectedFilter == FilterType.ALL,
            onClick = { onFilterSelected(FilterType.ALL, null) },
            label = { Text("All") }
        )

        // Favorites filter
        FilterChip(
            selected = selectedFilter == FilterType.FAVORITES,
            onClick = { onFilterSelected(FilterType.FAVORITES, null) },
            label = { Text("Favorites") },
            leadingIcon = {
                Icon(
                    imageVector = FontAwesomeIcons.Solid.Heart,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        // Type filters
        teaTypes.forEach { teaType ->
            FilterChip(
                selected = selectedFilter == FilterType.BY_TYPE && selectedTypeId == teaType.id,
                onClick = { onFilterSelected(FilterType.BY_TYPE, teaType.id) },
                label = { Text(teaType.name) }
            )
        }
    }
}

@Preview
@Composable
private fun TeaFilterChipsPreview() {
    LeafLogTheme {
        TeaFilterChips(
            teaTypes = listOf(
                TeaType(
                    id = "1",
                    name = "Black Tea",
                    defaultTemperatureCelsius = null,
                    colorHex = "",
                    isSystemDefault = true,
                    displayOrder = 1,
                    createdAt = Instant.fromEpochMilliseconds(1),
                    updatedAt = Instant.fromEpochMilliseconds(1),
                ),
                TeaType(
                    id = "2",
                    name = "Green Tea",
                    defaultTemperatureCelsius = null,
                    colorHex = "",
                    isSystemDefault = true,
                    displayOrder = 2,
                    createdAt = Instant.fromEpochMilliseconds(1),
                    updatedAt = Instant.fromEpochMilliseconds(1),
                )
            ),
            selectedFilter = FilterType.ALL,
            selectedTypeId = null,
            onFilterSelected = { _, ignore -> Unit }
        )
    }
}