package dev.jketterer.leaflog.presentation.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.PeriodComparison
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.session.GenerateInsightsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetAnalyticsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingActivityUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingTrendsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetSteepInsightsUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTeaTypeDistributionUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTopRatedTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetTopTeasUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetVesselDistributionUseCase
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
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class AnalyticsViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val generateInsightsUseCase: GenerateInsightsUseCase,
    private val getBrewingTrendsUseCase: GetBrewingTrendsUseCase,
    private val getTeaTypeDistributionUseCase: GetTeaTypeDistributionUseCase,
    private val getTopTeasUseCase: GetTopTeasUseCase,
    private val getBrewingActivityUseCase: GetBrewingActivityUseCase,
    private val getSteepInsightsUseCase: GetSteepInsightsUseCase,
    private val getTopRatedTeasUseCase: GetTopRatedTeasUseCase,
    private val getVesselDistributionUseCase: GetVesselDistributionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private val _navEvents = Channel<AnalyticsNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

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
                _navEvents.trySend(
                    AnalyticsNavEvent.NavigateToHistory(
                        filterDateStart = intent.date.toString(),
                        filterDateEnd = intent.date.toString(),
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

            is AnalyticsIntent.ToggleWaterUnit -> toggleWaterUnit()
        }
    }

    private fun toggleWaterUnit() {
        viewModelScope.launch {
            val prefs = preferencesRepository.getPreferences()
            preferencesRepository.updateVolumeUnit(prefs.volumeUnit.toggle())
            loadAnalytics()
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
                periodLabel = "${start.month.name.take(3)} ${start.day} - ${end.month.name.take(3)} ${end.day}",
            )
        }
        loadAnalytics()
    }

    private fun loadAnalytics() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val prefs = preferencesRepository.getPreferences()

                // Count total completed sessions for empty state check
                val allSessions = teaSessionRepository.getByDateRange(
                    Instant.DISTANT_PAST,
                    Clock.System.now(),
                )
                val totalCompleted = allSessions.count {
                    it.status == SessionStatus.COMPLETED && it.parentSessionId == null
                }

                val hasMinimumData = totalCompleted >= 10

                val currentState = _state.value
                val (start, end) = calculateDateRange(
                    currentState.selectedPeriod,
                    currentState.customDateRange,
                )

                val currentData = getAnalyticsUseCase(start, end)

                // Calculate previous period for comparison
                val periodDuration = end - start
                val previousStart = start - periodDuration
                val previousEnd = start
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
                    )
                } else {
                    null
                }

                val formattedWater =
                    VolumeFormatter.format(currentData.totalWaterMl, prefs.volumeUnit)
                val formattedTime = formatDuration(currentData.totalBrewingTime)

                // Load chart data
                val trendPoints = getBrewingTrendsUseCase(start, end)
                val teaTypeDistribution = getTeaTypeDistributionUseCase(start, end)
                val topTeas = getTopTeasUseCase(start, end)
                val activityCells =
                    getBrewingActivityUseCase(start, end, currentState.selectedPeriod)
                        .getOrElse { emptyList() }
                val topRatedTeas = getTopRatedTeasUseCase(start, end)
                val vesselDistribution = getVesselDistributionUseCase(start, end)

                val insights = generateInsightsUseCase(
                    current = currentData,
                    topTeas = topTeas,
                    steepInsights = steepInsights,
                    teaTypeDistribution = teaTypeDistribution,
                    topRatedTeas = topRatedTeas,
                    vesselDistribution = vesselDistribution,
                    activityCells = activityCells,
                    period = currentState.selectedPeriod,
                )

                _state.update {
                    it.copy(
                        analytics = currentData,
                        comparison = comparison,
                        insights = insights,
                        formattedWaterQuantity = formattedWater,
                        formattedBrewingTime = formattedTime,
                        isLoading = false,
                        hasMinimumData = hasMinimumData,
                        totalCompletedSessions = totalCompleted,
                        trendPoints = trendPoints,
                        teaTypeDistribution = teaTypeDistribution,
                        topTeas = topTeas,
                        activityCells = activityCells,
                        steepInsights = steepInsights,
                        topRatedTeas = topRatedTeas,
                        vesselDistribution = vesselDistribution,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load analytics: ${e.message}",
                    )
                }
            }
        }
    }

    companion object {
        fun calculateDateRange(
            period: AnalyticsPeriod,
            customRange: Pair<LocalDate, LocalDate>?,
        ): Pair<Instant, Instant> {
            val tz = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val today = now.toLocalDateTime(tz).date

            return when (period) {
                AnalyticsPeriod.LAST_7_DAYS -> {
                    val start = today.minus(6, DateTimeUnit.DAY)
                    start.atStartOfDayIn(tz) to now
                }

                AnalyticsPeriod.LAST_30_DAYS -> {
                    val start = today.minus(30, DateTimeUnit.DAY)
                    start.atStartOfDayIn(tz) to now
                }

                AnalyticsPeriod.LAST_90_DAYS -> {
                    val start = today.minus(90, DateTimeUnit.DAY)
                    start.atStartOfDayIn(tz) to now
                }

                AnalyticsPeriod.THIS_WEEK -> {
                    val dayOfWeek = today.dayOfWeek.ordinal // Monday = 0
                    val startOfWeek = today.minus(dayOfWeek + 1, DateTimeUnit.DAY)
                    val endOfWeek = startOfWeek.plus(7, DateTimeUnit.DAY)
                    startOfWeek.atStartOfDayIn(tz) to endOfWeek.atStartOfDayIn(tz)
                }

                AnalyticsPeriod.THIS_MONTH -> {
                    val firstOfMonth = LocalDate(today.year, today.month, 1)
                    val firstOfNextMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH)
                    firstOfMonth.atStartOfDayIn(tz) to firstOfNextMonth.atStartOfDayIn(tz)
                }

                AnalyticsPeriod.LAST_MONTH -> {
                    val firstOfThisMonth = LocalDate(today.year, today.month, 1)
                    val firstOfLastMonth = firstOfThisMonth.minus(1, DateTimeUnit.MONTH)
                    firstOfLastMonth.atStartOfDayIn(tz) to firstOfThisMonth.atStartOfDayIn(tz)
                }

                AnalyticsPeriod.THIS_YEAR -> {
                    val firstOfYear = LocalDate(today.year, 1, 1)
                    firstOfYear.atStartOfDayIn(tz) to now
                }

                AnalyticsPeriod.ALL_TIME -> {
                    Instant.DISTANT_PAST to now
                }

                AnalyticsPeriod.CUSTOM -> {
                    if (customRange != null) {
                        val (start, end) = customRange
                        start.atStartOfDayIn(tz) to end.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
                    } else {
                        // Fallback to this month
                        val firstOfMonth = LocalDate(today.year, today.month, 1)
                        firstOfMonth.atStartOfDayIn(tz) to now
                    }
                }
            }
        }

        fun formatDuration(duration: Duration): String {
            val totalMinutes = duration.inWholeMinutes
            return when {
                totalMinutes < 1 -> "${duration.inWholeSeconds}s"
                totalMinutes < 60 -> "${totalMinutes}m"
                else -> {
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
                }
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
