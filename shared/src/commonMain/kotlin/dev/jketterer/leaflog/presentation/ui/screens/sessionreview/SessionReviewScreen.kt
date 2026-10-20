package dev.jketterer.leaflog.presentation.ui.screens.sessionreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.domain.usecases.session.BrewDifference
import dev.jketterer.leaflog.domain.usecases.session.VsBest
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoGrid
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.review_done
import leaf_log.shared.generated.resources.review_method_saved
import leaf_log.shared.generated.resources.review_note_header
import leaf_log.shared.generated.resources.review_note_label
import leaf_log.shared.generated.resources.review_photos_header
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SessionReviewScreen(
    route: NavRoute.SessionReviewRoute,
    onNavigateToHome: (finishedSessionId: String) -> Unit,
    viewModel: SessionReviewViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionReviewNavEvent.NavigateToHome -> onNavigateToHome(event.finishedSessionId)
            }
        }
    }

    SessionReviewContent(state = state, onIntent = viewModel::onIntent, snackbarHostState = snackbarHostState)
}

@Composable
private fun SessionReviewContent(
    state: SessionReviewState,
    onIntent: (SessionReviewIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val session = state.session
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(state.tea?.name ?: "") },
            navigationIcon = {
                IconButton(onClick = { onIntent(SessionReviewIntent.Done) }) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                }
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                // Failed to load; the snackbar says why
                session == null -> {}

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Spacer(Modifier.height(4.dp))
                    SteepRowsSection(session.steeps, onSteepClick = { onIntent(SessionReviewIntent.SteepClicked(it)) })

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(stringResource(Res.string.review_note_header))
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { onIntent(SessionReviewIntent.NotesChanged(it)) },
                            label = { Text(stringResource(Res.string.review_note_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 8,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(stringResource(Res.string.review_photos_header))
                        PhotoGrid(
                            photos = session.steeps.first().photos,
                            onAddPhoto = { onIntent(SessionReviewIntent.PhotoAdded(it)) },
                            onRemovePhoto = { onIntent(SessionReviewIntent.PhotoRemoved(it)) },
                        )
                    }

                    state.vsBest?.let {
                        VsBestSection(it, session, state.vessel, state.bestVessel)
                    }

                    when {
                        state.offerMethod -> SaveMethodCard(
                            label = state.methodLabel,
                            onLabelChange = { onIntent(SessionReviewIntent.MethodLabelChanged(it)) },
                            onSave = { onIntent(SessionReviewIntent.SaveMethod) },
                        )

                        state.methodSaved -> Text(
                            text = stringResource(Res.string.review_method_saved),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
        if (session != null) {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { onIntent(SessionReviewIntent.Done) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                ) { Text(stringResource(Res.string.review_done)) }
            }
        }
    }

    if (state.editingSteepId != null) {
        SteepEditSheet(
            rating = state.editRating,
            notes = state.editNotes,
            onRatingChange = { onIntent(SessionReviewIntent.SteepRatingChanged(it)) },
            onNotesChange = { onIntent(SessionReviewIntent.SteepNotesChanged(it)) },
            onDismiss = { onIntent(SessionReviewIntent.DismissSteepEdit) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionReviewPreview() {
    val best = previewSession(id = "best", ratings = listOf(5f))
    LeafLogTheme {
        SessionReviewContent(
            state = SessionReviewState(
                session = previewSession(ratings = listOf(4f, 4f, null)),
                vsBest = VsBest.Versus(best, listOf(BrewDifference.TEMPERATURE)),
                offerMethod = true,
                methodLabel = "5g · 200ml · 2:00",
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}
