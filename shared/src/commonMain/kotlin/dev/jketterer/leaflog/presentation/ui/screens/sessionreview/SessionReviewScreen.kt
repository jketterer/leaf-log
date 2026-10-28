package dev.jketterer.leaflog.presentation.ui.screens.sessionreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.SteepTag
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.timeline
import dev.jketterer.leaflog.domain.usecases.session.BrewDifference
import dev.jketterer.leaflog.domain.usecases.session.VsBest
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoGrid
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.SteepEditSheet
import dev.jketterer.leaflog.presentation.ui.components.session.SteepTimelineSection
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.review_add_note
import leaf_log.shared.generated.resources.review_done
import leaf_log.shared.generated.resources.review_method_saved
import leaf_log.shared.generated.resources.review_method_saved_detail
import leaf_log.shared.generated.resources.review_method_updated
import leaf_log.shared.generated.resources.review_method_updated_detail
import leaf_log.shared.generated.resources.review_note_header
import leaf_log.shared.generated.resources.review_photos_header
import leaf_log.shared.generated.resources.tea_not_found
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

    SessionReviewContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
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
            title = {},
            navigationIcon = {
                IconButton(onClick = { onIntent(SessionReviewIntent.Done) }) {
                    Icon(
                        FeatherIcons.ArrowLeft,
                        contentDescription = stringResource(Res.string.navigate_up)
                    )
                }
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                // Failed to load; the snackbar says why
                session == null -> {}

                else -> SessionReviewBody(
                    state = state,
                    session = session,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        // Hidden while typing, so the card's Save button isn't squeezed out of the shrunken scroll area.
        // The inset height rather than isImeVisible, which is Android-only.
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        if (session != null && !imeVisible) {
            Surface {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Button(
                    onClick = { onIntent(SessionReviewIntent.Done) },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) { Text(stringResource(Res.string.review_done)) }
            }
        }
    }

    val editingSteep = state.session?.steeps?.firstOrNull { it.id == state.editingSteepId }
    if (editingSteep != null) {
        SteepEditSheet(
            steep = editingSteep,
            rating = state.editRating,
            notes = state.editNotes,
            vocabulary = state.vocabulary,
            onRatingChange = { onIntent(SessionReviewIntent.SteepRatingChanged(it)) },
            onNotesChange = { onIntent(SessionReviewIntent.SteepNotesChanged(it)) },
            onTagToggle = { onIntent(SessionReviewIntent.SteepTagToggled(it)) },
            onTagCreate = { onIntent(SessionReviewIntent.SteepTagCreated(it)) },
            onParametersChange = { time, temperature, water ->
                onIntent(SessionReviewIntent.SteepParametersChanged(time, temperature, water))
            },
            onDismiss = { onIntent(SessionReviewIntent.DismissSteepEdit) },
        )
    }
}

@Composable
private fun SessionReviewBody(
    state: SessionReviewState,
    session: SessionWithSteeps,
    onIntent: (SessionReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val teaColor = state.teaTypeColorHex?.let { teaTypeColor(it) }?.takeIf { it.isSpecified }
        ?: MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SessionReviewHeaderSection(
            teaName = state.tea?.name ?: stringResource(Res.string.tea_not_found),
            vesselName = state.vessel?.name,
            teaColor = teaColor,
            rating = session.effectiveRating,
            isNewBest = state.vsBest is VsBest.NewBest,
            session = session.session,
        )

        SteepTimelineSection(
            timeline = session.timeline(),
            totalBrewTime = session.totalBrewTime,
            vocabulary = state.vocabulary,
            onSteepClick = { onIntent(SessionReviewIntent.SteepClicked(it)) },
            showCarriedOver = true,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(stringResource(Res.string.review_note_header))
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onIntent(SessionReviewIntent.NotesChanged(it)) },
                placeholder = { Text(stringResource(Res.string.review_add_note)) },
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

        (state.vsBest as? VsBest.Versus)?.let {
            VsBestSection(it, session, state.vessel, state.bestVessel)
        }

        when {
            state.offerMethod -> SaveMethodCard(
                label = state.methodLabel,
                onLabelChange = { onIntent(SessionReviewIntent.MethodLabelChanged(it)) },
                onSave = { onIntent(SessionReviewIntent.SaveMethod) },
            )

            state.methodToUpdate != null -> UpdateMethodCard(
                methodName = state.methodToUpdate.label ?: state.tea?.name.orEmpty(),
                onUpdate = { onIntent(SessionReviewIntent.UpdateMethod) },
            )

            state.methodSaved -> MethodDoneCard(
                title = stringResource(Res.string.review_method_saved),
                detail = stringResource(Res.string.review_method_saved_detail),
            )

            state.methodUpdated -> MethodDoneCard(
                title = stringResource(Res.string.review_method_updated),
                detail = stringResource(Res.string.review_method_updated_detail),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionReviewPreview() {
    val best = previewSession(id = "best", ratings = listOf(5f))
    val session = previewSession(ratings = listOf(4f, 4f, null))
    val tags = listOf(
        listOf(SteepTag("floral", confirmed = true), SteepTag("honey", confirmed = true)),
        listOf(
            SteepTag("floral", confirmed = true),
            SteepTag("honey", confirmed = false),
            SteepTag("orchid", confirmed = true)
        ),
        listOf(SteepTag("floral", confirmed = false), SteepTag("orchid", confirmed = false)),
    )
    LeafLogTheme {
        SessionReviewContent(
            state = SessionReviewState(
                session = session.copy(steeps = session.steeps.mapIndexed { i, steep ->
                    steep.copy(
                        tags = tags[i]
                    )
                }),
                tea = Tea(
                    id = "tea-1",
                    name = "Tie Guan Yin",
                    teaTypeId = "oolong",
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                teaTypeColorHex = "#FF9800",
                vessel = BrewingVessel(
                    id = "vessel",
                    name = "Gaiwan",
                    iconName = null,
                    isSystemDefault = false,
                    displayOrder = 0,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                ),
                vocabulary = listOf(
                    TastingTag(
                        "floral",
                        key = "floral",
                        name = null,
                        isCustom = false,
                        displayOrder = 0
                    ),
                    TastingTag(
                        "honey",
                        key = "honey",
                        name = null,
                        isCustom = false,
                        displayOrder = 2
                    ),
                    TastingTag(
                        "orchid",
                        key = null,
                        name = "Orchid",
                        isCustom = true,
                        displayOrder = 23
                    ),
                ),
                vsBest = VsBest.Versus(best, listOf(BrewDifference.TEMPERATURE)),
                offerMethod = true,
                methodLabel = "5g · 200ml · 2:00",
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}
