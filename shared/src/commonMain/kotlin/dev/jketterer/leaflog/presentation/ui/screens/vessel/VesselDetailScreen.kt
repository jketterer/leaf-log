package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Archive
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.VesselProfile
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.FullscreenImageViewer
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.session.SessionInProgressDialog
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.archive
import leaf_log.shared.generated.resources.brew_with_vessel
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.delete_vessel_text
import leaf_log.shared.generated.resources.delete_vessel_title
import leaf_log.shared.generated.resources.edit_vessel
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.unarchive
import leaf_log.shared.generated.resources.vessel_not_found
import leaf_log.shared.generated.resources.vessel_nothing_brewed
import leaf_log.shared.generated.resources.vessel_nothing_brewed_body
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.days

@Composable
fun VesselDetailScreen(
    route: NavRoute.VesselDetailRoute,
    onNavigateBack: () -> Unit,
    onNavigateToEditVessel: (String) -> Unit,
    onNavigateToTea: (teaId: String) -> Unit,
    onNavigateToLogTea: (NavRoute.LogTeaRoute) -> Unit,
    onNavigateToTimer: (sessionId: String) -> Unit,
    viewModel: VesselDetailViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState, onAction = viewModel::onIntent)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is VesselDetailNavigationEvent.NavigateBack -> onNavigateBack()
                is VesselDetailNavigationEvent.NavigateToEditVessel -> onNavigateToEditVessel(event.vesselId)
                is VesselDetailNavigationEvent.NavigateToTea -> onNavigateToTea(event.teaId)
                is VesselDetailNavigationEvent.NavigateToLogTea -> onNavigateToLogTea(event.route)
                is VesselDetailNavigationEvent.NavigateToTimer -> onNavigateToTimer(event.sessionId)
            }
        }
    }

    VesselDetailContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun VesselDetailContent(
    state: VesselDetailState,
    onIntent: (VesselDetailIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    // Null in a preview, where no Koin application is running and resolving would throw.
    imageStorage: ImageStorage? = if (LocalInspectionMode.current) null else koinInject(),
) {
    val vessel = state.vessel
    val photo = vessel?.imagePath?.let { path -> imageStorage?.resolveImagePath(path) }
    var showPhoto by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val inset = Modifier.padding(horizontal = 16.dp)
    // The heading follows the photo when there is one
    val headingIndex = if (photo != null) 1 else 0
    val headingVisible by remember(headingIndex) {
        derivedStateOf { listState.firstVisibleItemIndex <= headingIndex }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = vessel != null && !headingVisible, enter = fadeIn(), exit = fadeOut()) {
                        Text(vessel?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(VesselDetailIntent.BackClicked) }) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = stringResource(Res.string.navigate_up))
                    }
                },
                actions = {
                    if (vessel != null) {
                        IconButton(onClick = { onIntent(VesselDetailIntent.EditVesselClicked) }) {
                            Icon(FeatherIcons.Edit2, contentDescription = stringResource(Res.string.edit_vessel))
                        }
                        VesselMenu(isArchived = vessel.isArchived, canDelete = !state.hasSessions, onIntent = onIntent)
                    }
                },
            )
        },
        bottomBar = {
            if (vessel != null) {
                VesselBottomBar(isArchived = vessel.isArchived, onIntent = onIntent)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            vessel == null -> EmptyState(
                message = stringResource(Res.string.vessel_not_found),
                modifier = Modifier.padding(padding),
            )

            // No side padding: the tea rows run edge to edge, as in the tea picker, and the rest inset
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (photo != null) {
                    item(key = "photo") { VesselPhoto(photo, onClick = { showPhoto = true }, modifier = inset) }
                }
                item(key = "heading") {
                    VesselHeading(
                        vessel = vessel,
                        dominantType = state.profile?.dominantType,
                        onlyType = state.profile?.onlyType == true,
                        showIcon = photo == null,
                        modifier = inset,
                    )
                }
                val profile = state.profile
                if (profile == null) {
                    item(key = "nothing_brewed") {
                        EmptyState(
                            message = stringResource(Res.string.vessel_nothing_brewed),
                            body = stringResource(Res.string.vessel_nothing_brewed_body),
                            // EmptyState fills and scrolls, so it needs a bounded height in a list
                            modifier = inset.heightIn(max = 320.dp),
                        )
                    }
                    return@LazyColumn
                }
                item(key = "stats") {
                    VesselStats(
                        sessions = profile.sessions,
                        teas = profile.teas.size,
                        lastBrewed = profile.lastBrewed,
                        modifier = inset,
                    )
                }
                if (profile.types.size > 1) {
                    item(key = "seasoned_with") {
                        SeasonedWithSection(profile.types, profile.sessions, modifier = Modifier.animateItem().then(inset))
                    }
                }
                item(key = "teas") {
                    VesselTeasSection(
                        teas = profile.teas,
                        onTeaClick = { onIntent(VesselDetailIntent.TeaClicked(it)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showPhoto && photo != null) {
        FullscreenImageViewer(photos = listOf(photo), initialIndex = 0, onDismiss = { showPhoto = false })
    }

    VesselDetailDialogs(state, onIntent)
}

@Composable
private fun VesselMenu(
    isArchived: Boolean,
    canDelete: Boolean,
    onIntent: (VesselDetailIntent) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(FeatherIcons.MoreVertical, contentDescription = stringResource(Res.string.more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (!isArchived) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.archive)) },
                    leadingIcon = { Icon(FeatherIcons.Archive, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onIntent(VesselDetailIntent.ArchiveVesselClicked)
                    },
                )
            }
            // A vessel a session used is archived instead
            if (canDelete) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.delete)) },
                    leadingIcon = { Icon(FeatherIcons.Trash2, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onIntent(VesselDetailIntent.DeleteVesselClicked)
                    },
                )
            }
        }
    }
}

/** Brew with it, or bring it back when it's archived. */
@Composable
private fun VesselBottomBar(
    isArchived: Boolean,
    onIntent: (VesselDetailIntent) -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        AnimatedContent(
            targetState = isArchived,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { archived ->
            if (archived) {
                FilledTonalButton(
                    onClick = { onIntent(VesselDetailIntent.UnarchiveVesselClicked) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(FeatherIcons.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.unarchive))
                }
            } else {
                Button(
                    onClick = { onIntent(VesselDetailIntent.BrewWithVesselClicked) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(FeatherIcons.Coffee, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.brew_with_vessel))
                }
            }
        }
    }
}

@Composable
private fun VesselDetailDialogs(
    state: VesselDetailState,
    onIntent: (VesselDetailIntent) -> Unit,
) {
    if (state.showDeleteConfirmation) {
        ConfirmDestructiveDialog(
            title = stringResource(Res.string.delete_vessel_title),
            text = stringResource(Res.string.delete_vessel_text, state.vessel?.name.orEmpty()),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = { onIntent(VesselDetailIntent.ConfirmDelete) },
            onDismiss = { onIntent(VesselDetailIntent.CancelDelete) },
        )
    }

    state.inProgressDialogState?.let { dialogState ->
        SessionInProgressDialog(
            teaName = dialogState.teaName,
            vesselName = dialogState.vesselName,
            session = dialogState.session,
            isProcessing = dialogState.isProcessing,
            onResume = { onIntent(VesselDetailIntent.ResumeInProgress) },
            onCompleteAndContinue = { onIntent(VesselDetailIntent.CompleteInProgressAndContinue) },
            onDiscardAndContinue = { onIntent(VesselDetailIntent.DiscardInProgressAndContinue) },
            onDismiss = { onIntent(VesselDetailIntent.DismissInProgressDialog) },
        )
    }
}

private val previewProfile = VesselProfile(
    sessions = 48,
    lastBrewed = PreviewInstant - 3.days,
    types = listOf(
        TeaTypeDistribution(previewSheng, 40, 83f),
        TeaTypeDistribution(previewShou, 8, 17f),
    ),
    dominantType = previewSheng,
    onlyType = false,
    teas = listOf(
        previewVesselTea("2019 Yiwu Gushu", previewSheng, 14, 3),
        previewVesselTea("2008 Menghai 7542", previewShou, 8, 14),
        previewVesselTea("Bulang Old Tree", previewSheng, 9, 21),
        previewVesselTea("Jingmai Spring", previewSheng, 7, 30),
        previewVesselTea("Mengku Bingdao", previewSheng, 6, 45),
        previewVesselTea("Old Lao Ban Zhang", previewSheng, 4, 90, finished = true),
    ),
)

@Preview(showBackground = true)
@Composable
private fun VesselDetailPreview() {
    LeafLogTheme {
        VesselDetailContent(
            state = VesselDetailState(vessel = previewVessel(), profile = previewProfile, hasSessions = true, isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselDetailMixedUsePreview() {
    LeafLogTheme {
        VesselDetailContent(
            state = VesselDetailState(
                vessel = previewVessel(),
                profile = previewProfile.copy(
                    sessions = 2,
                    types = listOf(TeaTypeDistribution(previewSheng, 1, 50f), TeaTypeDistribution(previewShou, 1, 50f)),
                    dominantType = null,
                    onlyType = false,
                    teas = listOf(
                        previewVesselTea("2019 Yiwu Gushu", previewSheng, 1, 3),
                        previewVesselTea("2008 Menghai 7542", previewShou, 1, 14),
                    ),
                ),
                hasSessions = true,
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselDetailArchivedPreview() {
    LeafLogTheme {
        VesselDetailContent(
            state = VesselDetailState(
                vessel = previewVessel(isArchived = true),
                profile = previewProfile,
                hasSessions = true,
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselDetailNeverUsedPreview() {
    LeafLogTheme {
        VesselDetailContent(
            state = VesselDetailState(vessel = previewVessel(), isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselDetailLoadingPreview() {
    LeafLogTheme {
        VesselDetailContent(state = VesselDetailState(), onIntent = {})
    }
}
