package dev.jketterer.leaflog.presentation.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TrendGranularity
import dev.jketterer.leaflog.domain.models.activity
import dev.jketterer.leaflog.domain.models.comparedTo
import dev.jketterer.leaflog.domain.models.summary
import dev.jketterer.leaflog.domain.models.teaTypeDistribution
import dev.jketterer.leaflog.domain.models.topRatedTeas
import dev.jketterer.leaflog.domain.models.topTeas
import dev.jketterer.leaflog.domain.models.trends
import dev.jketterer.leaflog.domain.models.vesselDistribution
import dev.jketterer.leaflog.domain.models.window
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_analytics
import kotlin.time.Clock

class AnalyticsViewModel(
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private val _navEvents = Channel<AnalyticsNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    init {
        viewModelScope.launch {
            val savedPeriod = preferencesRepository.getPreferences().analyticsPeriod
            _state.update { it.copy(selectedPeriod = savedPeriod, periodLabel = savedPeriod.label) }
            observeAnalytics()
        }
    }

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            is AnalyticsIntent.ShowPeriodSelector -> _state.update { it.copy(showPeriodSelector = true) }
            is AnalyticsIntent.HidePeriodSelector -> _state.update { it.copy(showPeriodSelector = false) }
            is AnalyticsIntent.SelectPeriod -> selectPeriod(intent.period)
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
    }

    /** Recomputes on every write and period change. Aggregating reads every session, so off Main. */
    private suspend fun observeAnalytics() {
        combine(
            teaSessionRepository.getAllSessionsFlow(),
            teaRepository.getAllFlow(),
            teaTypeRepository.getAllFlow(),
            brewingVesselRepository.getAllFlow(),
            _state.map { it.selectedPeriod }.distinctUntilChanged(),
        ) { sessions, teaList, teaTypeList, vesselList, period ->
            val teas = teaList.associateBy { it.id }
            val teaTypes = teaTypeList.associateBy { it.id }
            val (window, previousWindow) = period.window(Clock.System.now(), TimeZone.currentSystemDefault())
            val analytics = sessions.summary(window)
            val trends = sessions.trends(window)
            val totalCompleted = sessions.count { it.session.status == SessionStatus.COMPLETED }
            _state.update {
                it.copy(
                    analytics = analytics,
                    comparison = previousWindow?.let { previous ->
                        analytics.comparedTo(sessions.summary(previous))
                    },
                    isLoading = false,
                    hasMinimumData = totalCompleted >= MINIMUM_SESSIONS,
                    totalCompletedSessions = totalCompleted,
                    trendPoints = trends.points,
                    trendGranularity = trends.granularity,
                    teaTypeDistribution = sessions.teaTypeDistribution(window, teas, teaTypes),
                    topTeas = sessions.topTeas(window, teas),
                    // All time would start a cell at the distant past
                    activityCells = if (period == AnalyticsPeriod.ALL_TIME) {
                        emptyList()
                    } else {
                        val byWeek = period == AnalyticsPeriod.LAST_90_DAYS || period == AnalyticsPeriod.THIS_YEAR
                        sessions.activity(window, byWeek, teas, teaTypes)
                    },
                    topRatedTeas = sessions.topRatedTeas(window, teas),
                    vesselDistribution = sessions.vesselDistribution(window, vesselList.associateBy { v -> v.id }),
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .catch { e ->
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_analytics)
            }
            .collect()
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
