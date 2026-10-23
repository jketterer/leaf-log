package dev.jketterer.leaflog.presentation.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.PeriodComparison
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TrendGranularity
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.session.GetAnalyticsDateRangeUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetAnalyticsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingActivityUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingTrendsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetSteepInsightsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTeaTypeDistributionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTopRatedTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTopTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetVesselDistributionUseCase
import dev.jketterer.leaflog.presentation.ui.components.common.formatDuration
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_analytics
import kotlin.time.Clock

class AnalyticsViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val getBrewingTrendsUseCase: GetBrewingTrendsUseCase,
    private val getTeaTypeDistributionUseCase: GetTeaTypeDistributionUseCase,
    private val getTopTeasUseCase: GetTopTeasUseCase,
    private val getBrewingActivityUseCase: GetBrewingActivityUseCase,
    private val getSteepInsightsUseCase: GetSteepInsightsUseCase,
    private val getTopRatedTeasUseCase: GetTopRatedTeasUseCase,
    private val getVesselDistributionUseCase: GetVesselDistributionUseCase,
    private val getAnalyticsDateRangeUseCase: GetAnalyticsDateRangeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private val _navEvents = Channel<AnalyticsNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val savedPeriod = preferencesRepository.getPreferences().analyticsPeriod
            _state.update {
                it.copy(
                    selectedPeriod = savedPeriod,
                    periodLabel = savedPeriod.label,
                )
            }
            loadAnalytics()
        }
    }

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            is AnalyticsIntent.LoadAnalytics -> loadAnalytics()
            is AnalyticsIntent.ShowPeriodSelector -> _state.update { it.copy(showPeriodSelector = true) }
            is AnalyticsIntent.HidePeriodSelector -> _state.update { it.copy(showPeriodSelector = false) }
            is AnalyticsIntent.SelectPeriod -> selectPeriod(intent.period)
            is AnalyticsIntent.SelectCustomRange -> selectCustomRange(intent.start, intent.end)
            is AnalyticsIntent.NavigateToLogTea -> _navEvents.trySend(AnalyticsNavEvent.NavigateToLogTea)
            is AnalyticsIntent.TapTrendPoint -> {
                val granularity = _state.value.trendGranularity
                val startDate = intent.date
                val endDate = when (granularity) {
                    TrendGranularity.DAILY -> startDate
                    TrendGranularity.WEEKLY -> startDate.plus(6, DateTimeUnit.DAY)
                    TrendGranularity.MONTHLY -> {
                        startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                    }
                }
                _navEvents.trySend(
                    AnalyticsNavEvent.NavigateToHistory(
                        filterDateStart = startDate.toString(),
                        filterDateEnd = endDate.toString(),
                    )
                )
            }

            is AnalyticsIntent.TapTeaType -> {
                _navEvents.trySend(
                    AnalyticsNavEvent.NavigateToHistory(filterTeaTypeId = intent.teaTypeId)
                )
            }

            is AnalyticsIntent.TapTopTea -> {
                _navEvents.trySend(AnalyticsNavEvent.NavigateToTeaDetail(intent.teaId))
            }

            is AnalyticsIntent.TapVessel -> { /* no-op: no vessel detail screen yet */
            }

            is AnalyticsIntent.TapTopRatedTea -> {
                _navEvents.trySend(AnalyticsNavEvent.NavigateToTeaDetail(intent.teaId))
            }
        }
    }

    private fun selectPeriod(period: AnalyticsPeriod) {
        _state.update {
            it.copy(
                selectedPeriod = period,
                showPeriodSelector = false,
                periodLabel = period.label,
            )
        }
        viewModelScope.launch { preferencesRepository.updateAnalyticsPeriod(period) }
        loadAnalytics()
    }

    private fun selectCustomRange(start: LocalDate, end: LocalDate) {
        _state.update {
            it.copy(
                selectedPeriod = AnalyticsPeriod.CUSTOM,
                customDateRange = start to end,
                showPeriodSelector = false,
                periodLabel = AnalyticsPeriod.CUSTOM.label,
            )
        }
        loadAnalytics()
    }

    private fun loadAnalytics() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                // Count total completed sessions for empty state check
                val totalCompleted = teaSessionRepository.getAllSessions()
                    .count { it.session.status == SessionStatus.COMPLETED }

                val hasMinimumData = totalCompleted >= MINIMUM_SESSIONS

                val currentState = _state.value
                val (start, end, previousStart, previousEnd) = getAnalyticsDateRangeUseCase(
                    currentState.selectedPeriod,
                    currentState.customDateRange,
                    Clock.System.now(),
                    TimeZone.currentSystemDefault(),
                )

                val currentData = getAnalyticsUseCase(start, end)
                val previousData = getAnalyticsUseCase(previousStart, previousEnd)

                // Fetch steep insights for both periods (needed for comparison)
                val steepInsights = getSteepInsightsUseCase(start, end)
                val previousSteepInsights = getSteepInsightsUseCase(previousStart, previousEnd)

                fun pctChange(current: Double, previous: Double): Float? =
                    if (previous == 0.0) null else ((current - previous) / previous * 100.0).toFloat()

                val comparison = if (previousData.totalSessions > 0) {
                    PeriodComparison(
                        percentageChangeSessions = pctChange(
                            currentData.totalSessions.toDouble(),
                            previousData.totalSessions.toDouble(),
                        ),
                        percentageChangeBrewTime = pctChange(
                            currentData.totalBrewingTime.inWholeSeconds.toDouble(),
                            previousData.totalBrewingTime.inWholeSeconds.toDouble(),
                        ),
                        percentageChangeWater = pctChange(
                            currentData.totalWaterMl,
                            previousData.totalWaterMl,
                        ),
                        percentageChangeUniqueTeas = pctChange(
                            currentData.uniqueTeasCount.toDouble(),
                            previousData.uniqueTeasCount.toDouble(),
                        ),
                        percentageChangeAverageSteeps = pctChange(
                            steepInsights.averageSteepsPerSession.toDouble(),
                            previousSteepInsights.averageSteepsPerSession.toDouble(),
                        ),
                        percentageChangeNewTeas = pctChange(
                            steepInsights.newTeaDiscoveries.toDouble(),
                            previousSteepInsights.newTeaDiscoveries.toDouble(),
                        ),
                        percentageChangeTeaUsed = pctChange(
                            currentData.totalTeaGrams,
                            previousData.totalTeaGrams,
                        ),
                    )
                } else {
                    null
                }

                val formattedTime = formatDuration(currentData.totalBrewingTime)

                // Load chart data
                val brewingTrends = getBrewingTrendsUseCase(start, end)
                val trendPoints = brewingTrends.points
                val trendGranularity = brewingTrends.granularity
                val teaTypeDistribution = getTeaTypeDistributionUseCase(start, end)
                val topTeas = getTopTeasUseCase(start, end)
                val activityCells =
                    getBrewingActivityUseCase(start, end, currentState.selectedPeriod)
                        .getOrElse { emptyList() }
                val topRatedTeas = getTopRatedTeasUseCase(start, end)
                val vesselDistribution = getVesselDistributionUseCase(start, end)
                _state.update {
                    it.copy(
                        analytics = currentData,
                        comparison = comparison,
                        formattedBrewingTime = formattedTime,
                        isLoading = false,
                        hasMinimumData = hasMinimumData,
                        totalCompletedSessions = totalCompleted,
                        trendPoints = trendPoints,
                        trendGranularity = trendGranularity,
                        teaTypeDistribution = teaTypeDistribution,
                        topTeas = topTeas,
                        activityCells = activityCells,
                        steepInsights = steepInsights,
                        topRatedTeas = topRatedTeas,
                        vesselDistribution = vesselDistribution,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_analytics)
            }
        }
    }
}

sealed interface AnalyticsNavEvent {
    data object NavigateToLogTea : AnalyticsNavEvent
    data class NavigateToHistory(
        val filterTeaTypeId: String? = null,
        val filterDateStart: String? = null,
        val filterDateEnd: String? = null,
    ) : AnalyticsNavEvent

    data class NavigateToTeaDetail(val teaId: String) : AnalyticsNavEvent
}
