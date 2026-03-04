package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Plus
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UnitConverter
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.presentation.ui.components.analytics.hexToColor
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeaTypeListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditTeaType: (String) -> Unit,
    onNavigateToAddTeaType: () -> Unit,
    viewModel: TeaTypeListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()

    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is TeaTypeListNavigationEvent.NavigateToEdit -> {
                onNavigateToEditTeaType(event.teaTypeId)
                viewModel.onNavigationEventHandled()
            }
            is TeaTypeListNavigationEvent.NavigateToAdd -> {
                onNavigateToAddTeaType()
                viewModel.onNavigationEventHandled()
            }
            is TeaTypeListNavigationEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.onNavigationEventHandled()
            }
            null -> {}
        }
    }

    TeaTypeListContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeaTypeListContent(
    state: TeaTypeListState,
    onIntent: (TeaTypeListIntent) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onIntent(TeaTypeListIntent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tea Types") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(TeaTypeListIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(TeaTypeListIntent.AddClicked) },
            ) {
                Icon(FeatherIcons.Plus, contentDescription = "Add tea type")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.isLoading && state.teaTypes.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                state.teaTypes.isEmpty() -> {
                    Text(
                        text = "No tea types found",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = state.teaTypes,
                            key = { it.id },
                        ) { teaType ->
                            TeaTypeCard(
                                teaType = teaType,
                                userPreferences = state.userPreferences,
                                onClick = { onIntent(TeaTypeListIntent.TeaTypeClicked(teaType.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeaTypeCard(
    teaType: TeaType,
    userPreferences: UserPreferences,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Color dot
            val color = teaType.colorHex.hexToColor()
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (color == Color.Unspecified || teaType.colorHex == "#FFFDE7") {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = teaType.name,
                    style = MaterialTheme.typography.bodyLarge,
                )

                val details = buildList {
                    teaType.defaultTemperatureCelsius?.let { celsius ->
                        val displayTemp = UnitConverter.celsiusToDisplayTemperature(
                            celsius.toDouble(),
                            userPreferences.temperatureUnit,
                        )
                        add("$displayTemp${userPreferences.temperatureUnit.symbol}")
                    }
                }
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaTypeListScreenPreview() {
    LeafLogTheme {
        TeaTypeListContent(
            state = TeaTypeListState(
                teaTypes = listOf(
                    TeaType(
                        id = "1",
                        name = "Green",
                        colorHex = "#4CAF50",
                        defaultTemperatureCelsius = 80,
                    ),
                    TeaType(
                        id = "2",
                        name = "Black",
                        colorHex = "#795548",
                        defaultTemperatureCelsius = 100,
                    ),
                    TeaType(
                        id = "3",
                        name = "White",
                        colorHex = "#FFFDE7",
                        defaultTemperatureCelsius = 75,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaTypeListEmptyPreview() {
    LeafLogTheme {
        TeaTypeListContent(
            state = TeaTypeListState(),
            onIntent = {},
        )
    }
}
