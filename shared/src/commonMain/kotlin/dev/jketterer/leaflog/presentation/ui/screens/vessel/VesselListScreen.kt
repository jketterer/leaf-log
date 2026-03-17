package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Plus
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesselListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVesselDetail: (String) -> Unit,
    onNavigateToAddVessel: () -> Unit,
    viewModel: VesselListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is VesselListNavigationEvent.NavigateToVesselDetail -> {
                onNavigateToVesselDetail(event.vesselId)
                viewModel.onNavigationEventHandled()
            }

            is VesselListNavigationEvent.NavigateToAddVessel -> {
                onNavigateToAddVessel()
                viewModel.onNavigationEventHandled()
            }

            is VesselListNavigationEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.onNavigationEventHandled()
            }

            null -> {}
        }
    }

    // Show error snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onIntent(VesselListIntent.ClearError)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Brewing Vessels") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onIntent(VesselListIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.vessels.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    state.vessels.isEmpty() -> {
                        Text(
                            text = "No vessels found",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = state.vessels,
                                key = { it.id },
                            ) { vessel ->
                                VesselCard(
                                    vessel = vessel,
                                    volumeUnit = state.userPreferences.volumeUnit,
                                    onClick = { viewModel.onIntent(VesselListIntent.VesselClicked(vessel.id)) },
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { viewModel.onIntent(VesselListIntent.AddVesselClicked) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(FeatherIcons.Plus, contentDescription = "Add vessel")
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
