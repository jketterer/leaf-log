package dev.jketterer.leaflog.presentation.ui.components.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerFilter
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaPickerSheet
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.session.RatingSelector
import dev.jketterer.leaflog.presentation.ui.screens.history.HistoryFilters
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.clear_all
import leaf_log.shared.generated.resources.header_date_range
import leaf_log.shared.generated.resources.header_minimum_rating
import leaf_log.shared.generated.resources.header_tea
import leaf_log.shared.generated.resources.header_tea_type
import leaf_log.shared.generated.resources.header_vessel
import leaf_log.shared.generated.resources.history_filter_choose_tea
import leaf_log.shared.generated.resources.history_filters
import org.jetbrains.compose.resources.stringResource

private enum class DateRangePreset(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month"),
    ;

    fun toDateRange(): Pair<LocalDate, LocalDate> {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return when (this) {
            TODAY -> today to today
            LAST_7_DAYS -> today.minus(6, DateTimeUnit.DAY) to today
            THIS_MONTH -> today.minus(29, DateTimeUnit.DAY) to today
        }
    }
}

private fun isCustomDateRange(start: LocalDate?, end: LocalDate?): Boolean {
    return !(start == null && end == null) && DateRangePreset.entries.none { preset ->
        val (presetStart, presetEnd) = preset.toDateRange()
        start == presetStart && end == presetEnd
    }
}

private fun LocalDate.formatDisplay(): String =
    TimeFormatter.formatDate(this, DateSkeleton.MONTH_DAY_YEAR)

/**
 * History's filters. [brewedTeas] are the teas the Tea filter offers, most recently brewed first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterSheet(
    filters: HistoryFilters,
    teaTypes: List<TeaType>,
    brewedTeas: List<Tea>,
    vessels: List<BrewingVessel>,
    onFiltersChange: (HistoryFilters) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        HistoryFilterSheetContent(
            filters = filters,
            teaTypes = teaTypes,
            brewedTeas = brewedTeas,
            vessels = vessels,
            onFiltersChange = onFiltersChange,
            onClear = onClear,
        )
    }
}

@Composable
private fun HistoryFilterSheetContent(
    filters: HistoryFilters,
    teaTypes: List<TeaType>,
    brewedTeas: List<Tea>,
    vessels: List<BrewingVessel>,
    onFiltersChange: (HistoryFilters) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
                text = stringResource(Res.string.history_filters),
                style = MaterialTheme.typography.titleLarge,
            )
            if (filters.isActive) {
                TextButton(onClick = onClear) {
                    Text(stringResource(Res.string.clear_all))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tea Type section
        SectionHeader(stringResource(Res.string.header_tea_type))
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            teaTypes
                .sortedBy { it.displayOrder }
                .forEach { teaType ->
                    val selected = teaType.id in filters.teaTypeIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val teaTypeIds = if (selected) {
                                filters.teaTypeIds - teaType.id
                            } else {
                                filters.teaTypeIds + teaType.id
                            }
                            // Clear tea selection if it doesn't belong to the chosen types
                            val currentTea = brewedTeas.find { it.id == filters.teaId }
                            val teaFits = currentTea == null ||
                                teaTypeIds.isEmpty() ||
                                currentTea.teaTypeId in teaTypeIds
                            onFiltersChange(
                                filters.copy(
                                    teaTypeIds = teaTypeIds,
                                    teaId = filters.teaId.takeIf { teaFits },
                                ),
                            )
                        },
                        label = { Text(teaType.name) },
                    )
                }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tea section
        SectionHeader(stringResource(Res.string.header_tea))
        Spacer(modifier = Modifier.height(8.dp))
        TeaFilterChip(
            filters = filters,
            teaTypes = teaTypes,
            brewedTeas = brewedTeas,
            onFiltersChange = onFiltersChange,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Vessel section
        SectionHeader(stringResource(Res.string.header_vessel))
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            vessels
                .sortedBy { it.displayOrder }
                .forEach { vessel ->
                    val selected = vessel.id in filters.vesselIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onFiltersChange(
                                filters.copy(
                                    vesselIds = if (selected) {
                                        filters.vesselIds - vessel.id
                                    } else {
                                        filters.vesselIds + vessel.id
                                    },
                                ),
                            )
                        },
                        label = { Text(vessel.name) },
                    )
                }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date Range section
        SectionHeader(stringResource(Res.string.header_date_range))
        Spacer(modifier = Modifier.height(8.dp))

        val isCustom = isCustomDateRange(filters.dateRangeStart, filters.dateRangeEnd)
        var showDateRangePicker by remember { mutableStateOf(false) }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DateRangePreset.entries.forEach { preset ->
                val (presetStart, presetEnd) = preset.toDateRange()
                val selected =
                    filters.dateRangeStart == presetStart && filters.dateRangeEnd == presetEnd
                FilterChip(
                    selected = selected,
                    onClick = {
                        onFiltersChange(
                            filters.copy(
                                dateRangeStart = presetStart.takeUnless { selected },
                                dateRangeEnd = presetEnd.takeUnless { selected },
                            ),
                        )
                    },
                    label = { Text(preset.label) },
                )
            }
            FilterChip(
                selected = isCustom,
                onClick = {
                    if (isCustom) {
                        onFiltersChange(filters.copy(dateRangeStart = null, dateRangeEnd = null))
                    } else {
                        showDateRangePicker = true
                    }
                },
                label = {
                    if (isCustom) {
                        val start = filters.dateRangeStart?.formatDisplay() ?: ""
                        val end = filters.dateRangeEnd?.formatDisplay() ?: ""
                        Text("$start – $end")
                    } else {
                        Text("Custom")
                    }
                },
            )
        }

        if (showDateRangePicker) {
            val dateRangePickerState = rememberDateRangePickerState(
                initialSelectedStartDateMillis = filters.dateRangeStart
                    ?.atStartOfDayIn(TimeZone.UTC)
                    ?.toEpochMilliseconds(),
                initialSelectedEndDateMillis = filters.dateRangeEnd
                    ?.atStartOfDayIn(TimeZone.UTC)
                    ?.toEpochMilliseconds(),
            )
            Dialog(
                onDismissRequest = { showDateRangePicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { showDateRangePicker = false }) {
                                Icon(
                                    imageVector = FeatherIcons.X,
                                    contentDescription = "Close",
                                )
                            }
                            TextButton(
                                onClick = {
                                    val startMillis = dateRangePickerState.selectedStartDateMillis
                                    val endMillis = dateRangePickerState.selectedEndDateMillis
                                    if (startMillis != null && endMillis != null) {
                                        val start = Instant.fromEpochMilliseconds(startMillis)
                                            .toLocalDateTime(TimeZone.UTC).date
                                        val end = Instant.fromEpochMilliseconds(endMillis)
                                            .toLocalDateTime(TimeZone.UTC).date
                                        onFiltersChange(
                                            filters.copy(dateRangeStart = start, dateRangeEnd = end),
                                        )
                                    }
                                    showDateRangePicker = false
                                },
                                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                                        dateRangePickerState.selectedEndDateMillis != null,
                            ) {
                                Text("Save")
                            }
                        }
                        DateRangePicker(
                            state = dateRangePickerState,
                            modifier = Modifier.weight(1f),
                            title = null,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rating section
        SectionHeader(stringResource(Res.string.header_minimum_rating))
        Spacer(modifier = Modifier.height(8.dp))
        RatingSelector(
            rating = filters.minRating ?: 0f,
            onRatingChange = { rating ->
                onFiltersChange(filters.copy(minRating = rating.takeUnless { it == 0f }))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** Unset, it opens [TeaPickerSheet]; set, a tap clears it, as the custom date range does. */
@Composable
private fun TeaFilterChip(
    filters: HistoryFilters,
    teaTypes: List<TeaType>,
    brewedTeas: List<Tea>,
    onFiltersChange: (HistoryFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedTea = brewedTeas.find { it.id == filters.teaId }

    FilterChip(
        selected = filters.teaId != null,
        onClick = {
            if (filters.teaId != null) {
                onFiltersChange(filters.copy(teaId = null))
            } else {
                showPicker = true
            }
        },
        label = { Text(selectedTea?.name ?: stringResource(Res.string.history_filter_choose_tea)) },
        modifier = modifier,
    )

    if (showPicker) {
        var query by remember { mutableStateOf("") }
        var pickerFilter by remember {
            mutableStateOf(
                filters.teaTypeIds.singleOrNull()?.let(TeaPickerFilter::Type) ?: TeaPickerFilter.All,
            )
        }
        TeaPickerSheet(
            teas = brewedTeas,
            recentTeas = brewedTeas,
            teaTypes = teaTypes,
            query = query,
            filter = pickerFilter,
            selectedTeaId = filters.teaId,
            onQueryChanged = { query = it },
            onFilterChanged = { pickerFilter = it },
            onTeaSelected = { tea ->
                showPicker = false
                // A tea outside the chosen types would match nothing, so the types give way
                onFiltersChange(
                    filters.copy(
                        teaId = tea.id,
                        teaTypeIds = filters.teaTypeIds.takeIf { tea.teaTypeId in it }.orEmpty(),
                    ),
                )
            },
            onTeaCreated = null,
            onDismiss = { showPicker = false },
        )
    }
}

private val previewTeaTypes = listOf(
    TeaType(
        id = "1",
        name = "Green",
        displayOrder = 0,
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
    ),
    TeaType(
        id = "2",
        name = "Black",
        displayOrder = 1,
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
    ),
    TeaType(
        id = "3",
        name = "Oolong",
        displayOrder = 2,
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
    ),
)

private val previewBrewedTeas = listOf(
    Tea(
        id = "t1",
        name = "Dragon Well",
        teaTypeId = "1",
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
    ),
)

@Preview
@Composable
private fun HistoryFilterSheetContentPreview() {
    LeafLogTheme {
        HistoryFilterSheetContent(
            filters = HistoryFilters(),
            teaTypes = previewTeaTypes,
            brewedTeas = previewBrewedTeas,
            vessels = emptyList(),
            onFiltersChange = {},
            onClear = {},
        )
    }
}

@Preview
@Composable
private fun HistoryFilterSheetContentCustomDateRangePreview() {
    LeafLogTheme {
        HistoryFilterSheetContent(
            filters = HistoryFilters(
                dateRangeStart = LocalDate(2026, 1, 1),
                dateRangeEnd = LocalDate(2026, 3, 15),
            ),
            teaTypes = emptyList(),
            brewedTeas = emptyList(),
            vessels = emptyList(),
            onFiltersChange = {},
            onClear = {},
        )
    }
}

@Preview
@Composable
private fun HistoryFilterSheetContentActiveFiltersPreview() {
    LeafLogTheme {
        HistoryFilterSheetContent(
            filters = HistoryFilters(teaTypeIds = setOf("1", "2"), teaId = "t1", minRating = 3f),
            teaTypes = previewTeaTypes,
            brewedTeas = previewBrewedTeas,
            vessels = emptyList(),
            onFiltersChange = {},
            onClear = {},
        )
    }
}
