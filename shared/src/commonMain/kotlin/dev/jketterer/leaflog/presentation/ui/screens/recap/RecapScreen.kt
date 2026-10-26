package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.Season
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.presentation.ui.components.analytics.ActivityHeatmap
import dev.jketterer.leaflog.presentation.ui.components.recap.recapTitle
import dev.jketterer.leaflog.presentation.ui.components.recap.seasonLabel
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_busiest_month
import leaf_log.shared.generated.resources.recap_close
import leaf_log.shared.generated.resources.recap_months
import leaf_log.shared.generated.resources.recap_page
import leaf_log.shared.generated.resources.recap_sessions
import leaf_log.shared.generated.resources.recap_teas
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RecapScreen(
    route: NavRoute.RecapRoute,
    onClose: () -> Unit,
    onNavigateToHistory: (NavRoute.HistoryRoute) -> Unit,
    viewModel: RecapViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    RecapContent(
        state = state,
        onClose = onClose,
        onTapDay = { date ->
            onNavigateToHistory(
                NavRoute.HistoryRoute(filterDateStart = date.toString(), filterDateEnd = date.toString()),
            )
        },
        snackbarHostState = snackbarHostState,
    )
}

/** A page per [RecapState.pages], each filled with the next of three container colors. */
@Composable
private fun RecapContent(
    state: RecapState,
    onClose: () -> Unit,
    onTapDay: (LocalDate) -> Unit = {},
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
            val (container, content) = pageColors(index)
            Surface(
                color = container,
                contentColor = content,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val page = state.pages[index]) {
                    is RecapPage.Cover -> CoverPage(state.season, state.hemisphere, page)
                    is RecapPage.Rhythm -> RhythmPage(page, onTapDay)
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

/** Centered on the page when it fits, scrolling when it doesn't, as at 2× font. */
@Composable
private fun RecapPageColumn(content: @Composable () -> Unit) {
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

@Composable
private fun CoverPage(season: Season, hemisphere: Hemisphere?, cover: RecapPage.Cover) {
    RecapPageColumn {
        Text(
            text = seasonLabel(season, hemisphere),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = recapTitle(season, hemisphere),
            style = MaterialTheme.typography.displaySmall,
        )
        val sameYear = season.start.year == season.lastMonth.year
        Text(
            text = stringResource(
                Res.string.recap_months,
                TimeFormatter.formatDate(
                    season.start,
                    if (sameYear) DateSkeleton.MONTH else DateSkeleton.MONTH_YEAR,
                ),
                TimeFormatter.formatDate(season.lastMonth, DateSkeleton.MONTH_YEAR),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = cover.sessions.toString(),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = pluralStringResource(Res.plurals.recap_sessions, cover.sessions),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = cover.teas.toString(),
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = pluralStringResource(Res.plurals.recap_teas, cover.teas),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun RhythmPage(rhythm: RecapPage.Rhythm, onTapDay: (LocalDate) -> Unit) {
    RecapPageColumn {
        Text(
            text = rhythm.busiestMonthSessions.toString(),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = pluralStringResource(
                Res.plurals.recap_busiest_month,
                rhythm.busiestMonthSessions,
                TimeFormatter.formatDate(rhythm.busiestMonth, DateSkeleton.MONTH),
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))
        // On surface, so tea type colors read as they do in Analytics
        Surface(shape = MaterialTheme.shapes.large) {
            ActivityHeatmap(
                cells = rhythm.cells,
                period = AnalyticsPeriod.THIS_SEASON,
                onTapCell = onTapDay,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
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
                season = Season(LocalDate(2026, 6, 1)),
                hemisphere = Hemisphere.NORTHERN,
                pages = listOf(RecapPage.Cover(sessions = 64, teas = 17)),
            ),
            onClose = {},
        )
    }
}

@Preview
@Composable
private fun RecapRhythmPreview() {
    val start = LocalDate(2026, 6, 1)
    LeafLogTheme {
        RecapContent(
            state = RecapState(
                season = Season(start),
                hemisphere = Hemisphere.NORTHERN,
                pages = listOf(
                    RecapPage.Rhythm(
                        cells = List(92) { index ->
                            val sessions = (index * 7 % 5).coerceAtMost(3)
                            ActivityCell(
                                date = start.plus(index, DateTimeUnit.DAY),
                                dominantTeaTypeColorHex = if (sessions > 0) "#4CAF50" else null,
                                sessionCount = sessions,
                            )
                        },
                        busiestMonth = LocalDate(2026, 7, 1),
                        busiestMonthSessions = 28,
                    ),
                ),
            ),
            onClose = {},
        )
    }
}

@Preview
@Composable
private fun RecapContentAcrossNewYearPreview() {
    LeafLogTheme {
        RecapContent(
            state = RecapState(
                season = Season(LocalDate(2025, 12, 1)),
                hemisphere = Hemisphere.SOUTHERN,
                pages = listOf(RecapPage.Cover(sessions = 1, teas = 1)),
            ),
            onClose = {},
        )
    }
}
