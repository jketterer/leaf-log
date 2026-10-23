package dev.jketterer.leaflog.presentation.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.activity
import dev.jketterer.leaflog.domain.models.activityBucket
import dev.jketterer.leaflog.domain.models.comparedTo
import dev.jketterer.leaflog.domain.models.summary
import dev.jketterer.leaflog.domain.models.teaTypeDistribution
import dev.jketterer.leaflog.domain.models.topRatedTeas
import dev.jketterer.leaflog.domain.models.topTeas
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
import kotlinx.datetime.TimeZone
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
            _state.update { it.copy(selectedPeriod = savedPeriod) }
            observeAnalytics()
        }
    }

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            is AnalyticsIntent.SelectPeriod -> selectPeriod(intent.period)
            is AnalyticsIntent.NavigateToLogTea -> _navEvents.trySend(AnalyticsNavEvent.NavigateToLogTea)
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
        _state.update { it.copy(selectedPeriod = period) }
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
            _state.update {
                it.copy(
                    analytics = analytics,
                    comparison = previousWindow?.let { previous ->
                        analytics.comparedTo(sessions.summary(previous))
                    },
                    isLoading = false,
                    hasCompletedSession = sessions.any { s -> s.session.status == SessionStatus.COMPLETED },
                    teaTypeDistribution = sessions.teaTypeDistribution(window, teas, teaTypes),
                    topTeas = sessions.topTeas(window, teas),
                    activityCells = sessions.activity(window, period.activityBucket, teas, teaTypes),
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
