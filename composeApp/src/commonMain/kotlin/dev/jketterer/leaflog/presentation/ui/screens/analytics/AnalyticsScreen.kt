package dev.jketterer.leaflog.presentation.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.BarChart2
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Layers
import compose.icons.feathericons.Search
import compose.icons.feathericons.Star
import compose.icons.feathericons.Sunrise
import dev.jketterer.leaflog.domain.models.AnalyticsData
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.Insight
import dev.jketterer.leaflog.domain.models.InsightType
import dev.jketterer.leaflog.domain.models.PeriodComparison
import dev.jketterer.leaflog.domain.models.SteepInsights
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TopReSteepedTea
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.domain.models.TrendPoint
import dev.jketterer.leaflog.presentation.ui.components.analytics.BrewingActivityHeatmap
import dev.jketterer.leaflog.presentation.ui.components.analytics.BrewingTrendsChart
import dev.jketterer.leaflog.presentation.ui.components.analytics.InsightItem
import dev.jketterer.leaflog.presentation.ui.components.analytics.PeriodSelector
import dev.jketterer.leaflog.presentation.ui.components.analytics.SummaryCard
import dev.jketterer.leaflog.presentation.ui.components.analytics.TeaTypeDistributionChart
import dev.jketterer.leaflog.presentation.ui.components.analytics.TopRatedTeasChart
import dev.jketterer.leaflog.presentation.ui.components.analytics.TopTeasChart
import dev.jketterer.leaflog.presentation.ui.components.analytics.VesselDistributionChart
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@Composable
fun AnalyticsScreen(
    onNavigateToLogTea: () -> Unit,
    onNavigateToHistory: (filterTeaTypeId: String?, filterDateStart: String?, filterDateEnd: String?) -> Unit = { _, _, _ -> },
    onNavigateToTeaDetail: (teaId: String) -> Unit = {},
    viewModel: AnalyticsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                AnalyticsNavEvent.NavigateToLogTea -> onNavigateToLogTea()
                is AnalyticsNavEvent.NavigateToHistory -> onNavigateToHistory(
                    event.filterTeaTypeId,
                    event.filterDateStart,
                    event.filterDateEnd,
                )

                is AnalyticsNavEvent.NavigateToTeaDetail -> onNavigateToTeaDetail(event.teaId)
            }
        }
    }

    AnalyticsContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsContent(
    state: AnalyticsState,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Analytics") },
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            !state.hasMinimumData -> {
                AnalyticsEmptyState(
                    totalCompletedSessions = state.totalCompletedSessions,
                    onLogTeaClick = { onIntent(AnalyticsIntent.NavigateToLogTea) },
                )
            }

            else -> {
                AnalyticsDataContent(
                    state = state,
                    onIntent = onIntent,
                )
            }
        }

    }
}

@Composable
private fun AnalyticsEmptyState(
    totalCompletedSessions: Int,
    onLogTeaClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = FeatherIcons.BarChart2,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Not enough data yet",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$totalCompletedSessions / 10 sessions completed",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Log more tea sessions to unlock analytics and insights about your brewing habits.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLogTeaClick,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = FeatherIcons.Coffee,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Tea")
        }
    }
}

@Composable
private fun AnalyticsDataContent(
    state: AnalyticsState,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // Period selector (right-aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeriodSelector(
                currentLabel = state.periodLabel,
                expanded = state.showPeriodSelector,
                onExpandChange = { expanded ->
                    if (expanded) onIntent(AnalyticsIntent.ShowPeriodSelector)
                    else onIntent(AnalyticsIntent.HidePeriodSelector)
                },
                onPeriodSelected = { period -> onIntent(AnalyticsIntent.SelectPeriod(period)) },
            )
        }

        // Brewing activity heatmap (hidden for ALL_TIME)
        if (state.selectedPeriod != AnalyticsPeriod.ALL_TIME && state.activityCells.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            BrewingActivityHeatmap(
                cells = state.activityCells,
                period = state.selectedPeriod,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Summary cards - 2x2 grid
        val analytics = state.analytics
        if (analytics != null) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryCard(
                    value = analytics.totalSessions.toString(),
                    label = "Sessions",
                    icon = FeatherIcons.Coffee,
                    percentageChange = state.comparison?.percentageChangeSessions,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    value = state.formattedBrewingTime,
                    label = "Brew Time",
                    icon = FeatherIcons.Clock,
                    percentageChange = state.comparison?.percentageChangeBrewTime,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryCard(
                    value = state.formattedWaterQuantity,
                    label = "Water Used",
                    icon = FeatherIcons.Droplet,
                    percentageChange = state.comparison?.percentageChangeWater,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    value = analytics.uniqueTeasCount.toString(),
                    label = "Unique Teas",
                    icon = FeatherIcons.Star,
                    percentageChange = state.comparison?.percentageChangeUniqueTeas,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Steep insights cards
        val steepInsights = state.steepInsights
        if (steepInsights != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val averageSteeps = (steepInsights.averageSteepsPerSession * 10).roundToInt() / 10.0
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryCard(
                    value = averageSteeps.toString(),
                    label = "Average Steeps",
                    icon = FeatherIcons.Layers,
                    percentageChange = state.comparison?.percentageChangeAverageSteeps,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    value = steepInsights.newTeaDiscoveries.toString(),
                    label = "New Teas",
                    icon = FeatherIcons.Search,
                    percentageChange = state.comparison?.percentageChangeNewTeas,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Insights section
        if (state.insights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Insights",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.insights.forEach { insight ->
                InsightItem(
                    type = insight.type,
                    text = insight.text,
                )
            }
        }

        // Brewing Trends Chart
        Spacer(modifier = Modifier.height(24.dp))
        BrewingTrendsChart(
            trendPoints = state.trendPoints,
            onTapPoint = { date -> onIntent(AnalyticsIntent.TapTrendPoint(date)) },
        )

        // Tea Type Distribution Chart
        Spacer(modifier = Modifier.height(12.dp))
        TeaTypeDistributionChart(
            distribution = state.teaTypeDistribution,
            onTapSegment = { teaTypeId -> onIntent(AnalyticsIntent.TapTeaType(teaTypeId)) },
        )

        // Vessel Distribution Chart
        Spacer(modifier = Modifier.height(12.dp))
        VesselDistributionChart(
            distribution = state.vesselDistribution,
            onTapSegment = { vesselId -> onIntent(AnalyticsIntent.TapVessel(vesselId)) },
        )

        // Most Brewed Teas Chart
        Spacer(modifier = Modifier.height(12.dp))
        TopTeasChart(
            topTeas = state.topTeas,
            onTapTea = { teaId -> onIntent(AnalyticsIntent.TapTopTea(teaId)) },
        )

        // Highest Rated Teas Chart
        Spacer(modifier = Modifier.height(12.dp))
        TopRatedTeasChart(
            topRatedTeas = state.topRatedTeas,
            onTapTea = { teaId -> onIntent(AnalyticsIntent.TapTopRatedTea(teaId)) },
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Preview(showBackground = true)
@Composable
private fun AnalyticsEmptyStatePreview() {
    LeafLogTheme {
        AnalyticsEmptyState(
            totalCompletedSessions = 3,
            onLogTeaClick = {},
        )
    }
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
                ),
                comparison = PeriodComparison(
                    percentageChangeSessions = 10.5f,
                    percentageChangeBrewTime = 8.0f,
                    percentageChangeWater = 5.0f,
                    percentageChangeUniqueTeas = 25.0f,
                    percentageChangeAverageSteeps = -5.0f,
                    percentageChangeNewTeas = 33.0f,
                ),
                steepInsights = SteepInsights(
                    averageSteepsPerSession = 2.3f,
                    topReSteepedTea = TopReSteepedTea(
                        tea = Tea(
                            id = "1",
                            name = "Dragon Well",
                            teaTypeId = "1",
                            createdAt = now,
                            updatedAt = now,
                            syncStatus = SyncStatus.LOCAL_ONLY
                        ),
                        averageSteeps = 3.2f,
                        ),
                    newTeaDiscoveries = 4,
                ),
                insights = listOf(
                    Insight(
                        InsightType.MOST_BREWED,
                        "Dragon Well is your most brewed tea with 12 sessions",
                    ),
                    Insight(
                        InsightType.FAVORITE_TYPE,
                        "Green tea makes up 38% of your sessions",
                    ),
                    Insight(
                        InsightType.RE_STEEP,
                        "Dragon Well is your most re-steeped tea, averaging 3.2 steeps per session",
                    ),
                    Insight(
                        InsightType.TOP_RATED,
                        "Tie Guan Yin is your highest rated tea at 4.8 ★",
                    ),
                    Insight(
                        InsightType.PREFERRED_VESSEL,
                        "Gaiwan is your go-to vessel, used in 60% of sessions",
                    ),
                    Insight(
                        InsightType.CONSISTENCY,
                        "Your longest brewing streak was 5 days in a row",
                    ),
                ),
                formattedWaterQuantity = "5.2L",
                formattedBrewingTime = "2h 30m",
                periodLabel = "This Month",
                isLoading = false,
                hasMinimumData = true,
                trendPoints = listOf(
                    TrendPoint(LocalDate(2024, 1, 1), 2),
                    TrendPoint(LocalDate(2024, 1, 2), 4),
                    TrendPoint(LocalDate(2024, 1, 3), 1),
                    TrendPoint(LocalDate(2024, 1, 4), 6),
                    TrendPoint(LocalDate(2024, 1, 5), 3),
                    TrendPoint(LocalDate(2024, 1, 6), 5),
                    TrendPoint(LocalDate(2024, 1, 7), 2),
                ),
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
