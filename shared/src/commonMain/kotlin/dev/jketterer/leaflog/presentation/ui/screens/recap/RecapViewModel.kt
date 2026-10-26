package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.ActivityBucket
import dev.jketterer.leaflog.domain.models.Season
import dev.jketterer.leaflog.domain.models.activity
import dev.jketterer.leaflog.domain.models.busiestMonth
import dev.jketterer.leaflog.domain.models.summary
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load_sessions

/** Computed live from every session, so editing one in the season corrects its recap. */
class RecapViewModel(
    route: NavRoute.RecapRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecapState(Season(LocalDate.parse(route.seasonStart))))
    val state: StateFlow<RecapState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

    init {
        viewModelScope.launch {
            val season = _state.value.season
            combine(
                teaSessionRepository.getAllSessionsFlow(),
                teaRepository.getAllFlow(),
                teaTypeRepository.getAllFlow(),
                preferencesRepository.getPreferencesFlow(),
            ) { sessions, teas, teaTypes, preferences ->
                val window = season.window(TimeZone.currentSystemDefault())
                val summary = sessions.summary(window)
                val days = sessions.activity(
                    window,
                    ActivityBucket.DAY,
                    teas.associateBy { it.id },
                    teaTypes.associateBy { it.id },
                )
                _state.update {
                    it.copy(
                        hemisphere = preferences.hemisphere,
                        pages = listOfNotNull(
                            RecapPage.Cover(summary.totalSessions, summary.uniqueTeasCount),
                            days.busiestMonth()?.let { (month, count) -> RecapPage.Rhythm(days, month, count) },
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
