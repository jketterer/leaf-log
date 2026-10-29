package dev.jketterer.leaflog.presentation.ui.screens.flavors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.flavorProfile
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load

/** The whole journal's flavor profile, every tag ranked, for Collection's Flavors tab. */
class FlavorsViewModel(
    teaSessionRepository: TeaSessionRepository,
    tastingTagRepository: TastingTagRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FlavorsState())
    val state: StateFlow<FlavorsState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

    init {
        viewModelScope.launch {
            // Reads every session, so it runs off Main
            combine(teaSessionRepository.getAllSessionsFlow(), tastingTagRepository.getAllFlow()) { sessions, vocabulary ->
                FlavorsState(vocabulary, sessions.flavorProfile(vocabulary, limit = vocabulary.size), isLoading = false)
            }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _state.value = _state.value.copy(isLoading = false)
                    messages.failure(e, Res.string.couldnt_load)
                }
                .collect { _state.value = it }
        }
    }
}
