package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.Season
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.LocalDate
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_close
import leaf_log.shared.generated.resources.recap_page
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RecapScreen(
    route: NavRoute.RecapRoute,
    onClose: () -> Unit,
    onNavigateToHistory: (NavRoute.HistoryRoute) -> Unit,
    onNavigateToTea: (teaId: String) -> Unit,
    onNavigateToSession: (sessionId: String) -> Unit,
    viewModel: RecapViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    RecapContent(
        state = state,
        onClose = onClose,
        onTapDays = { days ->
            onNavigateToHistory(
                NavRoute.HistoryRoute(
                    filterDateStart = days.start.toString(),
                    filterDateEnd = days.endInclusive.toString(),
                ),
            )
        },
        onTapTea = onNavigateToTea,
        onTapSession = onNavigateToSession,
        snackbarHostState = snackbarHostState,
    )
}

/** A page per [RecapState.pages], each filled with the next of three container colors. */
@Composable
private fun RecapContent(
    state: RecapState,
    onClose: () -> Unit,
    onTapDays: (ClosedRange<LocalDate>) -> Unit = {},
    onTapTea: (teaId: String) -> Unit = {},
    onTapSession: (sessionId: String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val pagerState = rememberPagerState { state.pages.size }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            RecapPageSurface(index) {
                when (val page = state.pages[index]) {
                    is RecapPage.Cover -> CoverPage(state.period, state.hemisphere, page)
                    is RecapPage.Rhythm -> RhythmPage(page, onTapDays)
                    is RecapPage.MostBrewed -> MostBrewedPage(page, onTapTea)
                    is RecapPage.Discoveries -> DiscoveriesPage(page, onTapTea)
                    is RecapPage.Flavors -> FlavorsPage(page)
                    is RecapPage.HowYouBrewed -> HowYouBrewedPage(page, onTapSession)
                    is RecapPage.BestSession -> BestSessionPage(page, onTapSession)
                    is RecapPage.Shelf -> ShelfPage(page, onTapTea)
                }
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = FeatherIcons.X,
                contentDescription = stringResource(Res.string.recap_close),
                tint = pageColors(pagerState.currentPage).second,
            )
        }
        if (pagerState.pageCount > 1) {
            PageDots(
                count = pagerState.pageCount,
                current = pagerState.currentPage,
                color = pageColors(pagerState.currentPage).second,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The container and content colors of page [index], cycling primary, secondary and tertiary. */
@Composable
private fun pageColors(index: Int): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (index % 3) {
        0 -> primaryContainer to onPrimaryContainer
        1 -> secondaryContainer to onSecondaryContainer
        else -> tertiaryContainer to onTertiaryContainer
    }
}

/** Page [index] of the pager, filled with its container color. */
@Composable
internal fun RecapPageSurface(index: Int, content: @Composable () -> Unit) {
    val (container, contentColor) = pageColors(index)
    Surface(
        color = container,
        contentColor = contentColor,
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

/** Centered on the page when it fits, scrolling when it doesn't, as at 2× font. */
@Composable
internal fun RecapPageColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 56.dp),
    ) {
        content()
    }
}

/** A tea's name, opening Tea Detail. */
@Composable
internal fun RecapTeaRow(tea: Tea, onTapTea: (teaId: String) -> Unit) {
    Text(
        text = tea.name,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onTapTea(tea.id) }
            .wrapContentHeight(),
    )
}

@Composable
private fun PageDots(count: Int, current: Int, color: Color, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.recap_page, current + 1, count)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) color else color.copy(alpha = 0.3f)),
            )
        }
    }
}

@Preview
@Composable
private fun RecapContentPreview() {
    LeafLogTheme {
        RecapContent(
            state = RecapState(
                period = Season(LocalDate(2026, 6, 1)),
                hemisphere = Hemisphere.NORTHERN,
                pages = listOf(RecapPage.Cover(sessions = 64, teas = 17)),
            ),
            onClose = {},
        )
    }
}
