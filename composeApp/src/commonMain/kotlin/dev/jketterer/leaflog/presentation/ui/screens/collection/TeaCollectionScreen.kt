package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Search
import compose.icons.feathericons.Sliders
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaCard
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaFilterChips
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselCard
import dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListViewModel
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeaCollectionScreen(
    onNavigateToTeaDetail: (String) -> Unit,
    onNavigateToAddTea: () -> Unit,
    onNavigateToVesselDetail: (String) -> Unit,
    onNavigateToAddVessel: () -> Unit,
    viewModel: TeaCollectionViewModel = koinViewModel(),
    vesselListViewModel: VesselListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val vesselState by vesselListViewModel.state.collectAsState()

    TeaCollectionContent(
        state = state,
        vesselState = vesselState,
        onIntent = viewModel::onIntent,
        onNavigateToTeaDetail = onNavigateToTeaDetail,
        onNavigateToAddTea = onNavigateToAddTea,
        onNavigateToVesselDetail = onNavigateToVesselDetail,
        onNavigateToAddVessel = onNavigateToAddVessel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeaCollectionContent(
    state: TeaCollectionState,
    vesselState: dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListState,
    onIntent: (TeaCollectionIntent) -> Unit,
    onNavigateToTeaDetail: (String) -> Unit,
    onNavigateToAddTea: () -> Unit,
    onNavigateToVesselDetail: (String) -> Unit,
    onNavigateToAddVessel: () -> Unit,
) {
    var showSearchBar by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (showSearchBar && state.selectedTab == CollectionTab.TEAS) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { query ->
                        onIntent(TeaCollectionIntent.SearchQueryChanged(query))
                    },
                    onSearch = {},
                    active = true,
                    onActiveChange = { active ->
                        if (!active) {
                            showSearchBar = false
                            onIntent(TeaCollectionIntent.SearchQueryChanged(""))
                        }
                    },
                    placeholder = { Text("Search teas...") },
                    modifier = Modifier.fillMaxWidth()
                ) {}
            } else {
                TopAppBar(
                    title = { Text("Collection") },
                    actions = {
                        if (state.selectedTab == CollectionTab.TEAS) {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        imageVector = FeatherIcons.Sliders,
                                        contentDescription = "Sort teas",
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                ) {
                                    TeaSortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                onIntent(TeaCollectionIntent.SortSelected(option))
                                                showSortMenu = false
                                            },
                                            trailingIcon = if (state.selectedSortOption == option) {
                                                {
                                                    Icon(
                                                        imageVector = FeatherIcons.Check,
                                                        contentDescription = null,
                                                    )
                                                }
                                            } else null,
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { showSearchBar = true }) {
                                Icon(
                                    imageVector = FeatherIcons.Search,
                                    contentDescription = "Search teas",
                                )
                            }
                        }
                    }
                )
            }

            // Tab row
            PrimaryTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
            ) {
                Tab(
                    selected = state.selectedTab == CollectionTab.TEAS,
                    onClick = { onIntent(TeaCollectionIntent.TabSelected(CollectionTab.TEAS)) },
                    text = { Text("Teas") },
                )
                Tab(
                    selected = state.selectedTab == CollectionTab.VESSELS,
                    onClick = { onIntent(TeaCollectionIntent.TabSelected(CollectionTab.VESSELS)) },
                    text = { Text("Vessels") },
                )
            }

            when (state.selectedTab) {
                CollectionTab.TEAS -> TeasTabContent(
                    state = state,
                    onIntent = onIntent,
                    onNavigateToTeaDetail = onNavigateToTeaDetail,
                    onNavigateToAddTea = onNavigateToAddTea,
                )

                CollectionTab.VESSELS -> VesselsTabContent(
                    vesselState = vesselState,
                    onNavigateToVesselDetail = onNavigateToVesselDetail,
                    onNavigateToAddVessel = onNavigateToAddVessel,
                )
            }

            // Error message
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { onIntent(TeaCollectionIntent.ClearError) }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }

        // Contextual FAB
        FloatingActionButton(
            onClick = when (state.selectedTab) {
                CollectionTab.TEAS -> onNavigateToAddTea
                CollectionTab.VESSELS -> onNavigateToAddVessel
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = FeatherIcons.Plus,
                contentDescription = when (state.selectedTab) {
                    CollectionTab.TEAS -> "Add tea"
                    CollectionTab.VESSELS -> "Add vessel"
                },
            )
        }
    }
}

@Composable
private fun TeasTabContent(
    state: TeaCollectionState,
    onIntent: (TeaCollectionIntent) -> Unit,
    onNavigateToTeaDetail: (String) -> Unit,
    onNavigateToAddTea: () -> Unit,
) {
    // Filter chips
    TeaFilterChips(
        teaTypes = state.teaTypes,
        selectedFilter = state.selectedFilter,
        selectedTypeId = state.selectedTypeId,
        onFilterSelected = { filterType, typeId ->
            when (filterType) {
                FilterType.ALL -> onIntent(TeaCollectionIntent.ShowAllTeas)
                FilterType.FAVORITES -> onIntent(TeaCollectionIntent.ShowFavorites)
                FilterType.BY_TYPE -> typeId?.let {
                    onIntent(TeaCollectionIntent.FilterByType(it))
                }
            }
        }
    )

    // Content
    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.isEmpty -> {
            EmptyState(
                message = when (state.selectedFilter) {
                    FilterType.ALL -> "No teas in your collection yet"
                    FilterType.FAVORITES -> "No favorite teas yet"
                    FilterType.BY_TYPE -> "No teas of this type"
                },
                actionText = if (state.selectedFilter == FilterType.ALL) {
                    "Add Your First Tea"
                } else null,
                onActionClick = if (state.selectedFilter == FilterType.ALL) {
                    onNavigateToAddTea
                } else null
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = state.teas,
                    key = { it.id }
                ) { tea ->
                    val teaType = state.teaTypes.find { it.id == tea.teaTypeId }

                    TeaCard(
                        tea = tea,
                        teaTypeName = teaType?.name ?: "Unknown",
                        teaTypeColorHex = teaType?.colorHex,
                        onTeaClick = {
                            onIntent(TeaCollectionIntent.TeaClicked(tea.id))
                            onNavigateToTeaDetail(tea.id)
                        },
                        onFavoriteClick = {
                            onIntent(TeaCollectionIntent.ToggleFavorite(tea))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VesselsTabContent(
    vesselState: dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListState,
    onNavigateToVesselDetail: (String) -> Unit,
    onNavigateToAddVessel: () -> Unit,
) {
    when {
        vesselState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        vesselState.vessels.isEmpty() -> {
            EmptyState(
                message = "No vessels yet",
                actionText = "Add Your First Vessel",
                onActionClick = onNavigateToAddVessel,
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = vesselState.vessels,
                    key = { it.id },
                ) { vessel ->
                    VesselCard(
                        vessel = vessel,
                        volumeUnit = vesselState.userPreferences.volumeUnit,
                        onClick = { onNavigateToVesselDetail(vessel.id) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun TeaCollectionScreenPreview() {
    LeafLogTheme(useDarkTheme = false) {
        TeaCollectionContent(
            state = TeaCollectionState(),
            vesselState = dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListState(),
            onIntent = { _ -> },
            onNavigateToTeaDetail = { _ -> },
            onNavigateToAddTea = {},
            onNavigateToVesselDetail = { _ -> },
            onNavigateToAddVessel = {},
        )
    }
}
