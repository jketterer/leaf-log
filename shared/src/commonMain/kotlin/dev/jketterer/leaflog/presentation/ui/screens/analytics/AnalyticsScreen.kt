package dev.jketterer.leaflog.presentation.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.BarChart2
import compose.icons.feathericons.Bookmark
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Compass
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Feather
import compose.icons.feathericons.Layers
import compose.icons.feathericons.Package
import dev.jketterer.leaflog.domain.models.AnalyticsData
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.PeriodComparison
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.presentation.ui.components.analytics.BrewingActivityHeatmap
import dev.jketterer.leaflog.presentation.ui.components.analytics.PeriodSelector
import dev.jketterer.leaflog.presentation.ui.components.analytics.SummaryCard
import dev.jketterer.leaflog.presentation.ui.components.analytics.TeaTypeDistributionChart
import dev.jketterer.leaflog.presentation.ui.components.analytics.TopRatedTeasChart
import dev.jketterer.leaflog.presentation.ui.components.analytics.TopTeasChart
import dev.jketterer.leaflog.presentation.ui.components.analytics.VesselDistributionChart
import dev.jketterer.leaflog.presentation.ui.components.common.EmptyState
import dev.jketterer.leaflog.presentation.ui.components.common.LocalUnits
import dev.jketterer.leaflog.presentation.ui.components.common.UnitSwitchIcon
import dev.jketterer.leaflog.presentation.ui.components.common.formatDuration
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_average_steeps
import leaf_log.shared.generated.resources.analytics_average_tea
import leaf_log.shared.generated.resources.analytics_brew_time
import leaf_log.shared.generated.resources.analytics_empty_body
import leaf_log.shared.generated.resources.analytics_empty_title
import leaf_log.shared.generated.resources.analytics_new_teas
import leaf_log.shared.generated.resources.analytics_period_empty
import leaf_log.shared.generated.resources.analytics_previous_month
import leaf_log.shared.generated.resources.analytics_previous_season
import leaf_log.shared.generated.resources.analytics_previous_week
import leaf_log.shared.generated.resources.analytics_previous_year
import leaf_log.shared.generated.resources.analytics_sessions
import leaf_log.shared.generated.resources.analytics_tea_used
import leaf_log.shared.generated.resources.analytics_unique_teas
import leaf_log.shared.generated.resources.analytics_water_used
import leaf_log.shared.generated.resources.analytics_weighed_sessions
import leaf_log.shared.generated.resources.log_tea
import leaf_log.shared.generated.resources.tab_analytics
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalyticsScreen(
    onNavigateToLogTea: () -> Unit,
    onNavigateToHistory: (NavRoute.HistoryRoute) -> Unit,
    onNavigateToVessel: (vesselId: String) -> Unit,
    viewModel: AnalyticsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                AnalyticsNavEvent.NavigateToLogTea -> onNavigateToLogTea()
                is AnalyticsNavEvent.NavigateToHistory -> onNavigateToHistory(event.route)
                is AnalyticsNavEvent.NavigateToVessel -> onNavigateToVessel(event.vesselId)
            }
        }
    }

    AnalyticsContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun AnalyticsContent(
    state: AnalyticsState,
    onIntent: (AnalyticsIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.tab_analytics)) },
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                !state.hasCompletedSession -> {
                    EmptyState(
                        message = stringResource(Res.string.analytics_empty_title),
                        icon = FeatherIcons.BarChart2,
                        body = stringResource(Res.string.analytics_empty_body),
                        actionText = stringResource(Res.string.log_tea),
                        onActionClick = { onIntent(AnalyticsIntent.NavigateToLogTea) },
                    )
                }

                else -> {
                    AnalyticsDataContent(
                        state = state,
                        onIntent = onIntent,
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun AnalyticsDataContent(
    state: AnalyticsState,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PeriodSelector(
            selected = state.selectedPeriod,
            onPeriodSelected = { period -> onIntent(AnalyticsIntent.SelectPeriod(period)) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        val analytics = state.analytics
        if (analytics == null || analytics.totalSessions == 0) {
            EmptyState(
                message = stringResource(Res.string.analytics_period_empty),
                modifier = Modifier.weight(1f),
            )
        } else {
            AnalyticsCards(
                state = state,
                analytics = analytics,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AnalyticsCards(
    state: AnalyticsState,
    analytics: AnalyticsData,
    onIntent: (AnalyticsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        if (state.activityCells.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            BrewingActivityHeatmap(
                cells = state.activityCells,
                period = state.selectedPeriod,
                onTapCell = { date -> onIntent(AnalyticsIntent.TapActivityCell(date)) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val previousPeriod = previousPeriodLabel(state.selectedPeriod)

        // Summary cards - 2x2 grid
        var volumeSwitched by rememberSaveable { mutableStateOf(false) }
        var weightSwitched by rememberSaveable { mutableStateOf(false) }
        val volume = LocalUnits.current.volume.let { if (volumeSwitched) it.toggle() else it }
        val weight = LocalUnits.current.weight.let { if (weightSwitched) it.toggle() else it }
        val volumeChip = @Composable {
            UnitSwitchIcon(
                unit = volume,
                switched = volumeSwitched,
                onClick = { volumeSwitched = !volumeSwitched },
            )
        }
        val weightChip = @Composable {
            UnitSwitchIcon(
                unit = weight,
                switched = weightSwitched,
                onClick = { weightSwitched = !weightSwitched },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                value = analytics.totalSessions.toString(),
                label = stringResource(Res.string.analytics_sessions),
                icon = FeatherIcons.Coffee,
                percentageChange = state.comparison?.percentageChangeSessions,
                previousPeriod = previousPeriod,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                value = formatDuration(analytics.totalBrewingTime),
                label = stringResource(Res.string.analytics_brew_time),
                icon = FeatherIcons.Clock,
                percentageChange = state.comparison?.percentageChangeBrewTime,
                previousPeriod = previousPeriod,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                value = formatVolume(analytics.totalWaterMl, volume),
                label = stringResource(Res.string.analytics_water_used),
                icon = FeatherIcons.Droplet,
                percentageChange = state.comparison?.percentageChangeWater,
                previousPeriod = previousPeriod,
                onClick = { volumeSwitched = !volumeSwitched },
                action = volumeChip,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                value = analytics.uniqueTeasCount.toString(),
                label = stringResource(Res.string.analytics_unique_teas),
                icon = FeatherIcons.Bookmark,
                percentageChange = state.comparison?.percentageChangeUniqueTeas,
                previousPeriod = previousPeriod,
                modifier = Modifier.weight(1f),
            )
        }

        // Leaf usage cards. Hidden entirely when nothing in the period recorded a weight,
        // since "0.0 g" would read as a real measurement rather than an absent one.
        if (analytics.weighedSessionsCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryCard(
                    value = formatWeight(analytics.totalTeaGrams, weight),
                    label = stringResource(Res.string.analytics_tea_used),
                    icon = FeatherIcons.Package,
                    percentageChange = state.comparison?.percentageChangeTeaUsed,
                    previousPeriod = previousPeriod,
                    supportingText = if (analytics.weighedSessionsCount < analytics.totalSessions) {
                        pluralStringResource(
                            Res.plurals.analytics_weighed_sessions,
                            analytics.totalSessions,
                            analytics.weighedSessionsCount,
                            analytics.totalSessions,
                        )
                    } else {
                        null
                    },
                    onClick = { weightSwitched = !weightSwitched },
                    action = weightChip,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    value = analytics.averageTeaGrams?.let { formatWeight(it, weight) }.orEmpty(),
                    label = stringResource(Res.string.analytics_average_tea),
                    icon = FeatherIcons.Feather,
                    onClick = { weightSwitched = !weightSwitched },
                    action = weightChip,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Steep cards
        Spacer(modifier = Modifier.height(8.dp))
        val averageSteeps = ((analytics.averageSteeps ?: 0.0) * 10).roundToInt() / 10.0
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                value = averageSteeps.toString(),
                label = stringResource(Res.string.analytics_average_steeps),
                icon = FeatherIcons.Layers,
                percentageChange = state.comparison?.percentageChangeAverageSteeps,
                previousPeriod = previousPeriod,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                value = analytics.newTeasCount.toString(),
                label = stringResource(Res.string.analytics_new_teas),
                icon = FeatherIcons.Compass,
                percentageChange = state.comparison?.percentageChangeNewTeas,
                previousPeriod = previousPeriod,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.teaTypeDistribution.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            TeaTypeDistributionChart(
                distribution = state.teaTypeDistribution,
                onTapSegment = { teaTypeId -> onIntent(AnalyticsIntent.TapTeaType(teaTypeId)) },
            )
        }

        if (state.vesselDistribution.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            VesselDistributionChart(
                distribution = state.vesselDistribution,
                onTapSegment = { vesselId -> onIntent(AnalyticsIntent.TapVessel(vesselId)) },
            )
        }

        if (state.topTeas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            TopTeasChart(
                topTeas = state.topTeas,
                onTapTea = { teaId -> onIntent(AnalyticsIntent.TapTea(teaId)) },
            )
        }

        if (state.topRatedTeas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            TopRatedTeasChart(
                topRatedTeas = state.topRatedTeas,
                onTapTea = { teaId -> onIntent(AnalyticsIntent.TapTea(teaId)) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** The period a comparison is measured from; All time is compared with nothing. */
@Composable
private fun previousPeriodLabel(period: AnalyticsPeriod): String = when (period) {
    AnalyticsPeriod.THIS_WEEK -> stringResource(Res.string.analytics_previous_week)
    AnalyticsPeriod.THIS_MONTH -> stringResource(Res.string.analytics_previous_month)
    AnalyticsPeriod.THIS_SEASON -> stringResource(Res.string.analytics_previous_season)
    AnalyticsPeriod.THIS_YEAR -> stringResource(Res.string.analytics_previous_year)
    AnalyticsPeriod.ALL_TIME -> ""
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsDataContentPreview() {
    val now = Instant.fromEpochMilliseconds(0)
    LeafLogTheme {
        AnalyticsDataContent(
            state = AnalyticsState(
                selectedPeriod = AnalyticsPeriod.THIS_MONTH,
                analytics = AnalyticsData(
                    totalSessions = 42,
                    totalBrewingTime = 2.hours + 30.minutes,
                    totalWaterMl = 5200.0,
                    uniqueTeasCount = 8,
                    averageRating = 4.2f,
                    ratedSessionsCount = 35,
                    totalTeaGrams = 218.5,
                    weighedSessionsCount = 31,
                    totalSteeps = 97,
                    newTeasCount = 4,
                ),
                comparison = PeriodComparison(
                    percentageChangeSessions = 10.5f,
                    percentageChangeBrewTime = 8.0f,
                    percentageChangeWater = 5.0f,
                    percentageChangeUniqueTeas = 25.0f,
                    percentageChangeAverageSteeps = -5.0f,
                    percentageChangeNewTeas = 33.0f,
                    percentageChangeTeaUsed = 12.0f,
                ),
                isLoading = false,
                hasCompletedSession = true,
                teaTypeDistribution = listOf(
                    TeaTypeDistribution(
                        teaType = TeaType(id = "1", name = "Green", colorHex = "#4CAF50"),
                        sessionCount = 15,
                        percentage = 37.5f,
                    ),
                    TeaTypeDistribution(
                        teaType = TeaType(id = "2", name = "Black", colorHex = "#795548"),
                        sessionCount = 10,
                        percentage = 25f,
                    ),
                    TeaTypeDistribution(
                        teaType = TeaType(id = "3", name = "Oolong", colorHex = "#FF9800"),
                        sessionCount = 8,
                        percentage = 20f,
                    ),
                ),
                topTeas = listOf(
                    TopTea(
                        tea = Tea(
                            id = "1",
                            name = "Dragon Well",
                            teaTypeId = "1",
                            createdAt = now,
                            updatedAt = now,
                            syncStatus = SyncStatus.LOCAL_ONLY
                        ),
                        sessionCount = 12,
                    ),
                    TopTea(
                        tea = Tea(
                            id = "2",
                            name = "Tie Guan Yin",
                            teaTypeId = "2",
                            createdAt = now,
                            updatedAt = now,
                            syncStatus = SyncStatus.LOCAL_ONLY
                        ),
                        sessionCount = 8,
                    ),
                    TopTea(
                        tea = Tea(
                            id = "3",
                            name = "Earl Grey",
                            teaTypeId = "3",
                            createdAt = now,
                            updatedAt = now,
                            syncStatus = SyncStatus.LOCAL_ONLY
                        ),
                        sessionCount = 6,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsDataContentEmptyPeriodPreview() {
    LeafLogTheme {
        AnalyticsDataContent(
            state = AnalyticsState(
                isLoading = false,
                hasCompletedSession = true,
            ),
            onIntent = {},
        )
    }
}
