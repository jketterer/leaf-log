package dev.jketterer.leaflog.presentation.ui.components.collection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Search
import compose.icons.feathericons.Star
import compose.icons.feathericons.X
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.components.analytics.hexToColor
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.coroutines.launch
import kotlin.time.Instant

/** How many teas the shortcut sections show before falling through to "All teas". */
private const val SHORTCUT_LIMIT = 5

sealed interface TeaPickerFilter {
    data object All : TeaPickerFilter
    data object Favorites : TeaPickerFilter
    data class Type(val teaTypeId: String) : TeaPickerFilter
}

data class TeaPickerSection(
    val title: String,
    val teas: List<Tea>,
)

/**
 * Groups teas for the picker: shortcut sections when browsing, a single ranked list once a query or
 * chip narrows things down.
 */
internal fun buildTeaPickerSections(
    teas: List<Tea>,
    recentTeas: List<Tea>,
    teaTypes: List<TeaType>,
    query: String,
    filter: TeaPickerFilter,
): List<TeaPickerSection> {
    val trimmedQuery = query.trim()

    if (trimmedQuery.isBlank() && filter is TeaPickerFilter.All) {
        return listOf(
            TeaPickerSection("Recently brewed", recentTeas.take(SHORTCUT_LIMIT)),
            TeaPickerSection(
                title = "Favorites",
                teas = teas.filter { it.isFavorite }.sortedByName().take(SHORTCUT_LIMIT),
            ),
            TeaPickerSection("All teas", teas.sortedByName()),
        ).filter { it.teas.isNotEmpty() }
    }

    val matches = teas
        .filter { it.matchesFilter(filter) && it.matchesQuery(trimmedQuery) }
        .sortedWith(
            compareBy(
                { !it.name.startsWith(trimmedQuery, ignoreCase = true) },
                { it.name.lowercase() },
            ),
        )
    if (matches.isEmpty()) return emptyList()

    val title = when {
        trimmedQuery.isNotBlank() -> "Results"
        filter is TeaPickerFilter.Favorites -> "Favorites"
        filter is TeaPickerFilter.Type -> {
            val typeName = teaTypes.firstOrNull { it.id == filter.teaTypeId }?.name
            if (typeName != null) "$typeName teas" else "Results"
        }
        else -> "Results"
    }
    return listOf(TeaPickerSection(title, matches))
}

private fun List<Tea>.sortedByName() = sortedBy { it.name.lowercase() }

private fun Tea.matchesFilter(filter: TeaPickerFilter): Boolean = when (filter) {
    is TeaPickerFilter.All -> true
    is TeaPickerFilter.Favorites -> isFavorite
    is TeaPickerFilter.Type -> teaTypeId == filter.teaTypeId
}

private fun Tea.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return listOfNotNull(name, origin, producer).any { it.contains(query, ignoreCase = true) }
}

/**
 * Bottom sheet for picking a tea, with an option to quick-add a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeaPickerSheet(
    teas: List<Tea>,
    recentTeas: List<Tea>,
    teaTypes: List<TeaType>,
    query: String,
    filter: TeaPickerFilter,
    selectedTeaId: String?,
    onQueryChanged: (String) -> Unit,
    onFilterChanged: (TeaPickerFilter) -> Unit,
    onTeaSelected: (Tea) -> Unit,
    onAddTeaClicked: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
    ) {
        TeaPickerContent(
            teas = teas,
            recentTeas = recentTeas,
            teaTypes = teaTypes,
            query = query,
            filter = filter,
            selectedTeaId = selectedTeaId,
            onQueryChanged = onQueryChanged,
            onFilterChanged = onFilterChanged,
            onTeaSelected = { tea -> hideThen { onTeaSelected(tea) } },
            onAddTeaClicked = { hideThen(onAddTeaClicked) },
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeaPickerContent(
    teas: List<Tea>,
    recentTeas: List<Tea>,
    teaTypes: List<TeaType>,
    query: String,
    filter: TeaPickerFilter,
    selectedTeaId: String?,
    onQueryChanged: (String) -> Unit,
    onFilterChanged: (TeaPickerFilter) -> Unit,
    onTeaSelected: (Tea) -> Unit,
    onAddTeaClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val teaTypesById = remember(teaTypes) { teaTypes.associateBy { it.id } }
    val sections = remember(teas, recentTeas, teaTypes, query, filter) {
        buildTeaPickerSections(teas, recentTeas, teaTypes, query, filter)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            label = { Text("Search teas") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            leadingIcon = { Icon(FeatherIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(FeatherIcons.X, contentDescription = "Clear search")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )

        TeaPickerChips(
            teaTypes = teaTypes,
            filter = filter,
            onFilterChanged = onFilterChanged,
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                teas.isEmpty() -> EmptyState(
                    message = "Your collection is empty",
                    actionText = "Add your first tea",
                    onActionClick = onAddTeaClicked,
                )

                sections.isEmpty() && query.isNotBlank() -> TeaPickerMessage(
                    message = "No teas match “${query.trim()}”",
                )

                sections.isEmpty() -> TeaPickerMessage(
                    message = when (filter) {
                        is TeaPickerFilter.Favorites -> "No favorites yet"
                        is TeaPickerFilter.Type ->
                            "No ${teaTypesById[filter.teaTypeId]?.name?.lowercase() ?: ""} teas yet"

                        is TeaPickerFilter.All -> "No teas found"
                    },
                    actionText = "Show all teas",
                    onActionClick = { onFilterChanged(TeaPickerFilter.All) },
                )

                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    sections.forEach { section ->
                        stickyHeader(key = "header_${section.title}") {
                            TeaPickerSectionHeader(section.title)
                        }
                        items(section.teas, key = { "${section.title}_${it.id}" }) { tea ->
                            val teaType = teaTypesById[tea.teaTypeId]
                            TeaPickerRow(
                                tea = tea,
                                teaTypeName = teaType?.name ?: "",
                                teaTypeColorHex = teaType?.colorHex,
                                isSelected = tea.id == selectedTeaId,
                                onClick = { onTeaSelected(tea) },
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        TextButton(
            onClick = onAddTeaClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Icon(
                imageVector = FeatherIcons.Plus,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = if (query.isBlank()) {
                    "Add new tea"
                } else {
                    "Add “${query.trim()}”"
                },
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TeaPickerChips(
    teaTypes: List<TeaType>,
    filter: TeaPickerFilter,
    onFilterChanged: (TeaPickerFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = filter is TeaPickerFilter.All,
            onClick = { onFilterChanged(TeaPickerFilter.All) },
            label = { Text("All") },
        )
        FilterChip(
            selected = filter is TeaPickerFilter.Favorites,
            onClick = { onFilterChanged(TeaPickerFilter.Favorites) },
            label = { Text("Favorites") },
        )
        teaTypes.forEach { teaType ->
            FilterChip(
                selected = filter is TeaPickerFilter.Type && filter.teaTypeId == teaType.id,
                onClick = { onFilterChanged(TeaPickerFilter.Type(teaType.id)) },
                label = { Text(teaType.name) },
            )
        }
    }
}

@Composable
private fun TeaPickerSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    // Matches the sheet's own container colour so a pinned header reads as a label, not a band.
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            HorizontalDivider()
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun TeaPickerRow(
    tea: Tea,
    teaTypeName: String,
    teaTypeColorHex: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = teaTypeColorHex?.hexToColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accentColor != null) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Text(
                text = tea.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val details = listOfNotNull(teaTypeName.takeIf { it.isNotBlank() }, tea.origin)
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier.padding(end = 16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            tea.averageRating?.let { rating ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = FeatherIcons.Star,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = rating.formatOneDecimal(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (tea.totalSessions > 0) {
                Text(
                    text = "${tea.totalSessions} brews",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = FeatherIcons.Check,
                contentDescription = "Selected",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TeaPickerMessage(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionText)
            }
        }
    }
}

private val previewTeaTypes = listOf(
    TeaType(id = "green", name = "Green", colorHex = "#4CAF50"),
    TeaType(id = "black", name = "Black", colorHex = "#795548"),
)

private val previewTeas = listOf(
    previewTea("tea-1", "Dragon Well", "green", "China", 4.5f, 12, isFavorite = true),
    previewTea("tea-2", "Sencha", "green", "Japan", 4.1f, 8),
    previewTea("tea-3", "Assam Second Flush", "black", "India", 3.9f, 3),
    previewTea("tea-4", "Keemun", "black", "China", null, 0),
)

@Preview(showBackground = true)
@Composable
private fun TeaPickerSectionsPreview() {
    LeafLogTheme {
        SheetPreviewContainer {
            TeaPickerContent(
                teas = previewTeas,
                recentTeas = previewTeas.take(2),
                teaTypes = previewTeaTypes,
                query = "",
                filter = TeaPickerFilter.All,
                selectedTeaId = "tea-2",
                onQueryChanged = {},
                onFilterChanged = {},
                onTeaSelected = {},
                onAddTeaClicked = {},
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaPickerResultsPreview() {
    LeafLogTheme {
        SheetPreviewContainer {
            TeaPickerContent(
                teas = previewTeas,
                recentTeas = previewTeas.take(2),
                teaTypes = previewTeaTypes,
                query = "se",
                filter = TeaPickerFilter.All,
                selectedTeaId = null,
                onQueryChanged = {},
                onFilterChanged = {},
                onTeaSelected = {},
                onAddTeaClicked = {},
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaPickerNoResultsPreview() {
    LeafLogTheme {
        SheetPreviewContainer {
            TeaPickerContent(
                teas = previewTeas,
                recentTeas = previewTeas.take(2),
                teaTypes = previewTeaTypes,
                query = "Genmaicha",
                filter = TeaPickerFilter.All,
                selectedTeaId = null,
                onQueryChanged = {},
                onFilterChanged = {},
                onTeaSelected = {},
                onAddTeaClicked = {},
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaPickerEmptyCollectionPreview() {
    LeafLogTheme {
        SheetPreviewContainer {
            TeaPickerContent(
                teas = emptyList(),
                recentTeas = emptyList(),
                teaTypes = previewTeaTypes,
                query = "",
                filter = TeaPickerFilter.All,
                selectedTeaId = null,
                onQueryChanged = {},
                onFilterChanged = {},
                onTeaSelected = {},
                onAddTeaClicked = {},
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}

private fun previewTea(
    id: String,
    name: String,
    teaTypeId: String,
    origin: String,
    averageRating: Float?,
    totalSessions: Int,
    isFavorite: Boolean = false,
) = Tea(
    id = id,
    name = name,
    teaTypeId = teaTypeId,
    origin = origin,
    isFavorite = isFavorite,
    totalSessions = totalSessions,
    averageRating = averageRating,
    createdAt = Instant.fromEpochMilliseconds(1),
    updatedAt = Instant.fromEpochMilliseconds(1),
    syncStatus = SyncStatus.LOCAL_ONLY,
)
