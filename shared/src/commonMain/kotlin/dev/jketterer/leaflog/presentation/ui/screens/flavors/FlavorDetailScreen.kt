package dev.jketterer.leaflog.presentation.ui.screens.flavors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.MoreVertical
import dev.jketterer.leaflog.domain.models.FlavorUsage
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.dominantTags
import dev.jketterer.leaflog.presentation.ui.components.common.ConfirmDestructiveDialog
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.session.FlavorRow
import dev.jketterer.leaflog.presentation.ui.components.session.SessionCard
import dev.jketterer.leaflog.presentation.ui.components.session.YourTagIcon
import dev.jketterer.leaflog.presentation.ui.components.session.label
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.flavor_detail_sessions
import leaf_log.shared.generated.resources.flavor_not_tasted
import leaf_log.shared.generated.resources.flavor_not_tasted_body
import leaf_log.shared.generated.resources.header_most_with
import leaf_log.shared.generated.resources.header_recent_sessions
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.tag_delete_text
import leaf_log.shared.generated.resources.tag_delete_title
import leaf_log.shared.generated.resources.tag_merge
import leaf_log.shared.generated.resources.tag_merge_into
import leaf_log.shared.generated.resources.tag_merge_text
import leaf_log.shared.generated.resources.tag_merge_title
import leaf_log.shared.generated.resources.tag_no_sessions
import leaf_log.shared.generated.resources.tag_rename
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FlavorDetailScreen(
    route: NavRoute.FlavorDetailRoute,
    onNavigateBack: () -> Unit,
    onNavigateToFlavor: (tagId: String) -> Unit,
    onNavigateToTea: (teaId: String) -> Unit,
    onNavigateToSession: (sessionId: String) -> Unit,
    viewModel: FlavorDetailViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is FlavorDetailNavigationEvent.NavigateBack -> onNavigateBack()
                is FlavorDetailNavigationEvent.NavigateToFlavor -> onNavigateToFlavor(event.tagId)
            }
        }
    }

    FlavorDetailContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        onNavigateToTea = onNavigateToTea,
        onNavigateToSession = onNavigateToSession,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun FlavorDetailContent(
    state: FlavorDetailState,
    onIntent: (FlavorDetailIntent) -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToTea: (teaId: String) -> Unit = {},
    onNavigateToSession: (sessionId: String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val tag = state.tag
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (tag != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(tag.label(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (tag.isCustom) YourTagIcon()
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = stringResource(Res.string.navigate_up))
                    }
                },
                actions = { if (tag?.isCustom == true) TagMenu(onIntent) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val usage = state.usage
        if (tag == null || usage == null) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (usage.sessions == 0) {
                item(key = "not_tasted") {
                    EmptyState(
                        message = stringResource(Res.string.flavor_not_tasted),
                        body = stringResource(Res.string.flavor_not_tasted_body),
                        // EmptyState fills and scrolls, so it needs a bounded height in a list
                        modifier = Modifier.heightIn(max = 320.dp),
                    )
                }
                return@LazyColumn
            }
            item(key = "hero") { Hero(usage, modifier = Modifier.padding(bottom = 16.dp)) }
            item(key = "most_with_header") { SectionHeader(stringResource(Res.string.header_most_with)) }
            items(usage.teas, key = { "tea_${it.first}" }) { (teaId, count) ->
                val tea = state.teas[teaId] ?: return@items
                FlavorRow(
                    label = tea.name,
                    count = count,
                    outOf = usage.sessions,
                    onClick = { onNavigateToTea(teaId) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "recent_header") {
                SectionHeader(stringResource(Res.string.header_recent_sessions), modifier = Modifier.padding(top = 16.dp))
            }
            items(usage.recent, key = { it.session.id }) { session ->
                SessionCard(
                    session = session,
                    teaName = state.teas[session.session.teaId]?.name.orEmpty(),
                    tags = session.dominantTags(state.vocabulary),
                    onSessionClick = { onNavigateToSession(session.session.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    FlavorDialogs(state, onIntent)
}

/** How many sessions tasted it, large, as the Flavors tab shows its top flavor. */
@Composable
private fun Hero(usage: FlavorUsage, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = usage.sessions.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = pluralStringResource(Res.plurals.flavor_detail_sessions, usage.sessions, usage.sessions, usage.tagged),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun TagMenu(onIntent: (FlavorDetailIntent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(FeatherIcons.MoreVertical, contentDescription = stringResource(Res.string.more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                Res.string.tag_rename to FlavorDetailIntent.RenameClicked,
                Res.string.tag_merge_into to FlavorDetailIntent.MergeClicked,
                Res.string.delete to FlavorDetailIntent.DeleteClicked,
            ).forEach { (label, intent) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        expanded = false
                        onIntent(intent)
                    },
                )
            }
        }
    }
}

@Composable
private fun FlavorDialogs(
    state: FlavorDetailState,
    onIntent: (FlavorDetailIntent) -> Unit,
) {
    val tag = state.tag ?: return
    val sessions = state.usage?.sessions ?: 0
    val dismiss = { onIntent(FlavorDetailIntent.DialogDismissed) }
    when (val dialog = state.dialog) {
        null -> {}

        FlavorDialog.Rename -> RenameTagSheet(
            tag = tag,
            vocabulary = state.vocabulary,
            enabled = !state.isSaving,
            onRename = { onIntent(FlavorDetailIntent.RenameConfirmed(it)) },
            onMergeInstead = { onIntent(FlavorDetailIntent.MergeTargetChosen(it)) },
            onDismiss = dismiss,
        )

        FlavorDialog.MergePicker -> MergeTagSheet(
            tag = tag,
            vocabulary = state.vocabulary,
            onPick = { onIntent(FlavorDetailIntent.MergeTargetChosen(it)) },
            onDismiss = dismiss,
        )

        is FlavorDialog.ConfirmMerge -> {
            val target = dialog.target.label()
            ConfirmDestructiveDialog(
                title = stringResource(Res.string.tag_merge_title, tag.label(), target),
                text = if (sessions == 0) stringResource(Res.string.tag_no_sessions)
                else pluralStringResource(Res.plurals.tag_merge_text, sessions, sessions, target),
                confirmLabel = stringResource(Res.string.tag_merge),
                onConfirm = { onIntent(FlavorDetailIntent.RemovalConfirmed) },
                onDismiss = dismiss,
            )
        }

        FlavorDialog.ConfirmDelete -> ConfirmDestructiveDialog(
            title = stringResource(Res.string.tag_delete_title, tag.label()),
            text = if (sessions == 0) stringResource(Res.string.tag_no_sessions)
            else pluralStringResource(Res.plurals.tag_delete_text, sessions, sessions),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = { onIntent(FlavorDetailIntent.RemovalConfirmed) },
            onDismiss = dismiss,
        )
    }
}

private val previewTag = TastingTag(id = "peachy", key = null, name = "Peachy", isCustom = true, displayOrder = 23)

@Preview(showBackground = true)
@Composable
private fun FlavorDetailPreview() {
    LeafLogTheme {
        FlavorDetailContent(
            state = FlavorDetailState(
                tag = previewTag,
                vocabulary = listOf(previewTag),
                usage = FlavorUsage(sessions = 4, tagged = 52, teas = emptyList(), recent = emptyList()),
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FlavorDetailNotTastedPreview() {
    LeafLogTheme {
        FlavorDetailContent(
            state = FlavorDetailState(
                tag = previewTag,
                vocabulary = listOf(previewTag),
                usage = FlavorUsage(sessions = 0, tagged = 52, teas = emptyList(), recent = emptyList()),
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}
