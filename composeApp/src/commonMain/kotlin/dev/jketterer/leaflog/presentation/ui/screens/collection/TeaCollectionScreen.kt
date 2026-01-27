package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Snackbar
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
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Eye
import compose.icons.fontawesomeicons.solid.Plus
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaCard
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaFilterChips
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeaCollectionScreen(
    onNavigateToTeaDetail: (String) -> Unit,
    onNavigateToAddTea: () -> Unit,
    viewModel: TeaCollectionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    TeaCollectionContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToTeaDetail = onNavigateToTeaDetail,
        onNavigateToAddTea = onNavigateToAddTea
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeaCollectionContent(
    state: TeaCollectionState,
    onIntent: (TeaCollectionIntent) -> Unit,
    onNavigateToTeaDetail: (String) -> Unit,
    onNavigateToAddTea: () -> Unit
) {
    var showSearchBar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showSearchBar) {
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
                    title = { Text("Tea Library") },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(
                                imageVector = FontAwesomeIcons.Regular.Eye,
                                contentDescription = "Search teas"
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTea,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = FontAwesomeIcons.Solid.Plus,
                    contentDescription = "Add tea"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                                onTeaClick = {
                                    onIntent(TeaCollectionIntent.TeaClicked(tea.id))
                                    onNavigateToTeaDetail(tea.id)
                                },
                                onFavoriteClick = {
                                    onIntent(TeaCollectionIntent.ToggleFavorite(tea))
                                }
                            )
                        }
                    }
                }
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
    }
}

@Preview
@Composable
private fun TeaCollectionScreenPreview() {
    LeafLogTheme(useDarkTheme = false) {
        TeaCollectionContent(
            state = TeaCollectionState(),
            onIntent = { _ -> },
            onNavigateToTeaDetail = { _ -> },
            onNavigateToAddTea = {},
        )
    }
}