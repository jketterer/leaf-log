package dev.jketterer.leaflog.presentation.ui.components.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.screens.history.HistoryIntent
import dev.jketterer.leaflog.presentation.ui.screens.history.HistoryState
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

private enum class DateRangePreset(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ;

    fun toDateRange(): Pair<LocalDate, LocalDate> {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return when (this) {
            TODAY -> today to today
            THIS_WEEK -> today.minus(6, DateTimeUnit.DAY) to today
            THIS_MONTH -> today.minus(29, DateTimeUnit.DAY) to today
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterSheet(
    state: HistoryState,
    onIntent: (HistoryIntent) -> Unit,
    sheetState: SheetState,
) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(HistoryIntent.HideFilterSheet) },
        sheetState = sheetState,
    ) {
        HistoryFilterSheetContent(
            state = state,
            onIntent = onIntent,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterSheetContent(
    state: HistoryState,
    onIntent: (HistoryIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleLarge,
            )
            if (state.hasActiveFilters) {
                TextButton(onClick = { onIntent(HistoryIntent.ClearFilters) }) {
                    Text("Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tea Type section
        FilterSectionHeader("Tea Type")
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.teaTypes.values
                .sortedBy { it.displayOrder }
                .forEach { teaType ->
                    val selected = state.selectedTeaTypeId == teaType.id
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) {
                                onIntent(HistoryIntent.FilterByTeaType(null))
                            } else {
                                onIntent(HistoryIntent.FilterByTeaType(teaType.id))
                                // Clear tea selection if it doesn't belong to the new type
                                val currentTea = state.selectedTeaId?.let { state.teas[it] }
                                if (currentTea != null && currentTea.teaTypeId != teaType.id) {
                                    onIntent(HistoryIntent.FilterByTea(null))
                                }
                            }
                        },
                        label = { Text(teaType.name) },
                    )
                }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tea section
        FilterSectionHeader("Tea")
        Spacer(modifier = Modifier.height(8.dp))
        TeaDropdownFilter(
            teas = state.teas.values.toList(),
            selectedTeaId = state.selectedTeaId,
            selectedTeaTypeId = state.selectedTeaTypeId,
            onTeaSelected = { teaId -> onIntent(HistoryIntent.FilterByTea(teaId)) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date Range section
        FilterSectionHeader("Date Range")
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DateRangePreset.entries.forEach { preset ->
                val (presetStart, presetEnd) = preset.toDateRange()
                val selected =
                    state.dateRangeStart == presetStart && state.dateRangeEnd == presetEnd
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (selected) {
                            onIntent(HistoryIntent.FilterByDateRange(null, null))
                        } else {
                            onIntent(HistoryIntent.FilterByDateRange(presetStart, presetEnd))
                        }
                    },
                    label = { Text(preset.label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rating section
        FilterSectionHeader("Minimum Rating")
        Spacer(modifier = Modifier.height(8.dp))
        RatingSelector(
            rating = state.minRating ?: 0f,
            onRatingChange = { rating ->
                onIntent(HistoryIntent.FilterByMinRating(if (rating == 0f) null else rating))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status section
        FilterSectionHeader("Status")
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.showInProgressOnly,
                onClick = {
                    onIntent(HistoryIntent.ToggleShowInProgressOnly(!state.showInProgressOnly))
                },
                label = { Text("In Progress Only") },
            )
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeaDropdownFilter(
    teas: List<Tea>,
    selectedTeaId: String?,
    selectedTeaTypeId: String?,
    onTeaSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val filteredTeas = remember(teas, selectedTeaTypeId) {
        if (selectedTeaTypeId != null) {
            teas.filter { it.teaTypeId == selectedTeaTypeId }
        } else {
            teas
        }.sortedBy { it.name }
    }

    val selectedTeaName = selectedTeaId?.let { id -> teas.find { it.id == id }?.name } ?: "All Teas"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedTeaName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("All Teas") },
                onClick = {
                    onTeaSelected(null)
                    expanded = false
                },
            )
            filteredTeas.forEach { tea ->
                DropdownMenuItem(
                    text = { Text(tea.name) },
                    onClick = {
                        onTeaSelected(tea.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun HistoryFilterSheetContentPreview() {
    LeafLogTheme {
        HistoryFilterSheetContent(
            state = HistoryState(
                teaTypes = mapOf(
                    "1" to TeaType(
                        id = "1",
                        name = "Green",
                        displayOrder = 0,
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0),
                    ),
                    "2" to TeaType(
                        id = "2",
                        name = "Black",
                        displayOrder = 1,
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0),
                    ),
                    "3" to TeaType(
                        id = "3",
                        name = "Oolong",
                        displayOrder = 2,
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0),
                    ),
                ),
                teas = mapOf(
                    "t1" to Tea(
                        id = "t1",
                        name = "Dragon Well",
                        teaTypeId = "1",
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0),
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun HistoryFilterSheetContentActiveFiltersPreview() {
    LeafLogTheme {
        HistoryFilterSheetContent(
            state = HistoryState(
                selectedTeaTypeId = "1",
                minRating = 3f,
                showInProgressOnly = true,
                teaTypes = mapOf(
                    "1" to TeaType(
                        id = "1",
                        name = "Green",
                        displayOrder = 0,
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0),
                    ),
                    "2" to TeaType(
                        id = "2",
                        name = "Black",
                        displayOrder = 1,
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0),
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}
