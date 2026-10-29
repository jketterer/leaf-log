package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Search
import compose.icons.feathericons.Sliders
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.X
import dev.jketterer.leaflog.domain.models.TeaSortOption
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaCard
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaFilterChips
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselCard
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListIntent
import dev.jketterer.leaflog.presentation.ui.screens.flavors.FlavorsState
import dev.jketterer.leaflog.presentation.ui.screens.flavors.FlavorsTab
import dev.jketterer.leaflog.presentation.ui.screens.flavors.FlavorsViewModel
import dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListViewModel
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.add_first_tea
import leaf_log.shared.generated.resources.collection_empty
import leaf_log.shared.generated.resources.collection_no_favorites
import leaf_log.shared.generated.resources.collection_no_type
import leaf_log.shared.generated.resources.collection_none_dormant
import leaf_log.shared.generated.resources.collection_tab_flavors
import leaf_log.shared.generated.resources.collection_tab_teas
import leaf_log.shared.generated.resources.collection_tab_vessels
import leaf_log.shared.generated.resources.no_teas_found
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeaCollectionScreen(
    onNavigateToTeaDetail: (String) -> Unit,
    onNavigateToAddTea: () -> Unit,
    onNavigateToVesselDetail: (String) -> Unit,
    onNavigateToAddVessel: () -> Unit,
    onNavigateToFlavor: (tagId: String) -> Unit,
    viewModel: TeaCollectionViewModel = koinViewModel(),
    vesselListViewModel: VesselListViewModel = koinViewModel(),
    flavorsViewModel: FlavorsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val vesselState by vesselListViewModel.state.collectAsState()
    val flavorsState by flavorsViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)
    UiMessageEffect(vesselListViewModel.messages, snackbarHostState)
    UiMessageEffect(flavorsViewModel.messages, snackbarHostState)

    TeaCollectionContent(
        state = state,
        vesselState = vesselState,
        flavorsState = flavorsState,
        onIntent = viewModel::onIntent,
        onVesselIntent = vesselListViewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onNavigateToTeaDetail = onNavigateToTeaDetail,
        onNavigateToAddTea = onNavigateToAddTea,
        onNavigateToVesselDetail = onNavigateToVesselDetail,
        onNavigateToAddVessel = onNavigateToAddVessel,
        onNavigateToFlavor = onNavigateToFlavor,
    )
}

@Composable
private fun TeaCollectionContent(
    state: TeaCollectionState,
    vesselState: dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListState,
    flavorsState: FlavorsState,
    onIntent: (TeaCollectionIntent) -> Unit,
    onVesselIntent: (VesselListIntent) -> Unit,
    onNavigateToTeaDetail: (String) -> Unit,
    onNavigateToAddTea: () -> Unit,
    onNavigateToVesselDetail: (String) -> Unit,
    onNavigateToAddVessel: () -> Unit,
    onNavigateToFlavor: (tagId: String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showSearchBar by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                if (showSearchBar && state.selectedTab == CollectionTab.TEAS) {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                showSearchBar = false
                                onIntent(TeaCollectionIntent.SearchQueryChanged(""))
                            }) {
                                Icon(
                                    imageVector = FeatherIcons.ArrowLeft,
                                    contentDescription = "Close search",
                                )
                            }
                        },
                        title = {
                            TextField(
                                value = state.searchQuery,
                                onValueChange = { query ->
                                    onIntent(TeaCollectionIntent.SearchQueryChanged(query))
                                },
                                placeholder = { Text("Search teas...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                            )
                        },
                        actions = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    onIntent(TeaCollectionIntent.SearchQueryChanged(""))
                                }) {
                                    Icon(
                                        imageVector = FeatherIcons.X,
                                        contentDescription = "Clear search",
                                    )
                                }
                            }
                        },
                    )
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
                        text = { Text(stringResource(Res.string.collection_tab_teas)) },
                    )
                    Tab(
                        selected = state.selectedTab == CollectionTab.VESSELS,
                        onClick = { onIntent(TeaCollectionIntent.TabSelected(CollectionTab.VESSELS)) },
                        text = { Text(stringResource(Res.string.collection_tab_vessels)) },
                    )
                    Tab(
                        selected = state.selectedTab == CollectionTab.FLAVORS,
                        onClick = { onIntent(TeaCollectionIntent.TabSelected(CollectionTab.FLAVORS)) },
                        text = { Text(stringResource(Res.string.collection_tab_flavors)) },
                    )
                }
            }
        },
        floatingActionButton = {
            // Flavors come from tagging steeps, not from here
            val add = when (state.selectedTab) {
                CollectionTab.TEAS -> onNavigateToAddTea to "Add tea"
                CollectionTab.VESSELS -> onNavigateToAddVessel to "Add vessel"
                CollectionTab.FLAVORS -> null
            }
            if (add != null) {
                FloatingActionButton(onClick = add.first) {
                    Icon(imageVector = FeatherIcons.Plus, contentDescription = add.second)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state.selectedTab) {
                CollectionTab.TEAS -> TeasTabContent(
                    state = state,
                    onIntent = onIntent,
                    onNavigateToTeaDetail = onNavigateToTeaDetail,
                    onNavigateToAddTea = onNavigateToAddTea,
                )

                CollectionTab.VESSELS -> VesselsTabContent(
                    vesselState = vesselState,
                    onVesselIntent = onVesselIntent,
                    onNavigateToVesselDetail = onNavigateToVesselDetail,
                    onNavigateToAddVessel = onNavigateToAddVessel,
                )

                CollectionTab.FLAVORS -> FlavorsTab(state = flavorsState, onFlavorClick = onNavigateToFlavor)
            }
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
    TeaFilterChips(
        teaTypes = state.teaTypes,
        selectedFilter = state.selectedFilter,
        selectedTypeId = state.selectedTypeId,
        notBrewedLately = state.notBrewedLately,
        onNotBrewedLatelyClick = { onIntent(TeaCollectionIntent.ToggleNotBrewedLately) },
        onFilterSelected = { filterType, typeId ->
            when (filterType) {
                FilterType.ALL -> onIntent(TeaCollectionIntent.ShowAllTeas)
                FilterType.FAVORITES -> onIntent(TeaCollectionIntent.ShowFavorites)
                FilterType.BY_TYPE -> typeId?.let {
                    onIntent(TeaCollectionIntent.FilterByType(it))
                }
            }
        },
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
            val noTeas = state.searchQuery.isBlank() && !state.notBrewedLately &&
                state.selectedFilter == FilterType.ALL
            EmptyState(
                message = stringResource(
                    when {
                        state.searchQuery.isNotBlank() -> Res.string.no_teas_found
                        state.notBrewedLately -> Res.string.collection_none_dormant
                        else -> when (state.selectedFilter) {
                            FilterType.ALL -> Res.string.collection_empty
                            FilterType.FAVORITES -> Res.string.collection_no_favorites
                            FilterType.BY_TYPE -> Res.string.collection_no_type
                        }
                    }
                ),
                actionText = if (noTeas) stringResource(Res.string.add_first_tea) else null,
                onActionClick = if (noTeas) onNavigateToAddTea else null,
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

                // Bottom padding for FAB
                item(key = "bottom_padding") {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun VesselsTabContent(
    vesselState: dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListState,
    onVesselIntent: (VesselListIntent) -> Unit,
    onNavigateToVesselDetail: (String) -> Unit,
    onNavigateToAddVessel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !vesselState.showArchived,
                onClick = {
                    if (vesselState.showArchived) {
                        onVesselIntent(dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListIntent.ToggleShowArchived)
                    }
                },
                label = { Text("Active") },
            )
            FilterChip(
                selected = vesselState.showArchived,
                onClick = {
                    if (!vesselState.showArchived) {
                        onVesselIntent(dev.jketterer.leaflog.presentation.ui.screens.vessel.VesselListIntent.ToggleShowArchived)
                    }
                },
                label = { Text("All") },
            )
        }

        when {
            vesselState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            vesselState.vessels.isEmpty() -> {
                EmptyState(
                    message = if (vesselState.showArchived) "No vessels yet" else "No active vessels",
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
                            onClick = { onNavigateToVesselDetail(vessel.id) },
                        )
                    }
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
            flavorsState = FlavorsState(),
            onIntent = { _ -> },
            onVesselIntent = { _ -> },
            onNavigateToTeaDetail = { _ -> },
            onNavigateToAddTea = {},
            onNavigateToVesselDetail = { _ -> },
            onNavigateToAddVessel = {},
            onNavigateToFlavor = {},
        )
    }
}
