package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.ActivityBucket
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.CalendarYear
import dev.jketterer.leaflog.domain.models.activity
import dev.jketterer.leaflog.domain.models.activityBucket
import dev.jketterer.leaflog.domain.models.best
import dev.jketterer.leaflog.domain.models.brewingHabits
import dev.jketterer.leaflog.domain.models.busiestMonth
import dev.jketterer.leaflog.domain.models.discoveries
import dev.jketterer.leaflog.domain.models.flavorProfile
import dev.jketterer.leaflog.domain.models.newSince
import dev.jketterer.leaflog.domain.models.shelf
import dev.jketterer.leaflog.domain.models.steepCurve
import dev.jketterer.leaflog.domain.models.summary
import dev.jketterer.leaflog.domain.models.teaTypeDistribution
import dev.jketterer.leaflog.domain.models.topTeas
import dev.jketterer.leaflog.domain.models.vesselDistribution
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_sessions

/** Computed live from every session, so editing one in the period corrects its recap. */
class RecapViewModel(
    route: NavRoute.RecapRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val preferencesRepository: PreferencesRepository,
    private val tastingTagRepository: TastingTagRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecapState(route.period))
    val state: StateFlow<RecapState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

    init {
        viewModelScope.launch {
            val period = _state.value.period
            val heatmap = if (period is CalendarYear) AnalyticsPeriod.THIS_YEAR else AnalyticsPeriod.THIS_SEASON
            combine(
                teaSessionRepository.getAllSessionsFlow(),
                teaRepository.getAllFlow(),
                teaTypeRepository.getAllFlow(),
                preferencesRepository.getPreferencesFlow(),
                brewingVesselRepository.getAllFlow(),
            ) { sessions, teas, teaTypes, preferences, vessels ->
                val tz = TimeZone.currentSystemDefault()
                val window = period.window(tz)
                val previousWindow = period.previous.window(tz)
                val teasById = teas.associateBy { it.id }
                val teaTypesById = teaTypes.associateBy { it.id }
                val summary = sessions.summary(window)
                val days = sessions.activity(window, ActivityBucket.DAY, teasById, teaTypesById)
                val topTeas = sessions.topTeas(window, teasById).take(3)
                val discoveries = sessions.discoveries(window, teasById)
                // The vocabulary is read again each time, so a tag made since shows by name
                val vocabulary = tastingTagRepository.getAll()
                val flavors = sessions.filter { it.startedAt in window }.flavorProfile(vocabulary)
                    ?.takeIf { it.sessions >= 3 }
                val habits = sessions.brewingHabits(window)
                _state.update { state ->
                    state.copy(
                        hemisphere = preferences.hemisphere,
                        pages = listOfNotNull(
                            RecapPage.Cover(summary.totalSessions, summary.uniqueTeasCount),
                            days.busiestMonth()?.let { (month, count) ->
                                val cells = sessions.activity(window, heatmap.activityBucket, teasById, teaTypesById)
                                RecapPage.Rhythm(cells, heatmap, month, count)
                            },
                            topTeas.takeIf { it.isNotEmpty() }?.let {
                                RecapPage.MostBrewed(it, sessions.teaTypeDistribution(window, teasById, teaTypesById))
                            },
                            discoveries.takeIf { it.isNotEmpty() }?.let { RecapPage.Discoveries(it) },
                            flavors?.let { profile ->
                                val before = sessions.filter { it.startedAt in previousWindow }.flavorProfile(vocabulary)
                                RecapPage.Flavors(profile, profile.newSince(before))
                            },
                            habits?.let {
                                RecapPage.HowYouBrewed(
                                    habits = it,
                                    longestTeaName = teasById[it.longest.session.teaId]?.name,
                                    peakSteep = sessions.filter { s -> s.startedAt in window }.steepCurve()?.peak,
                                    favoriteVessel = sessions.vesselDistribution(window, vessels.associateBy { v -> v.id })
                                        .firstOrNull(),
                                )
                            },
                            sessions.filter { it.startedAt in window }.best()?.let { best ->
                                teasById[best.session.teaId]?.let { RecapPage.BestSession(best, it.name) }
                            },
                            sessions.shelf(window, teasById)
                                .takeIf { it.finished.isNotEmpty() || it.waiting.isNotEmpty() }
                                ?.let { RecapPage.Shelf(it.finished, it.waiting) },
                        ),
                    )
                }
            }
                .flowOn(Dispatchers.Default)
                .catch { e -> messages.failure(e, Res.string.couldnt_load_sessions) }
                .collect()
        }
    }
}
