package dev.jketterer.leaflog.presentation.ui.screens.flavors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.usageOf
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.RemoveTastingTagUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_delete_tag
import leaf_log.shared.generated.resources.couldnt_load
import leaf_log.shared.generated.resources.couldnt_merge_tag
import leaf_log.shared.generated.resources.couldnt_rename_tag

class FlavorDetailViewModel(
    private val route: NavRoute.FlavorDetailRoute,
    teaSessionRepository: TeaSessionRepository,
    teaRepository: TeaRepository,
    private val tastingTagRepository: TastingTagRepository,
    private val removeTastingTagUseCase: RemoveTastingTagUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(FlavorDetailState())
    val state: StateFlow<FlavorDetailState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

    private val _navEvents = Channel<FlavorDetailNavigationEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Reads every session, so it runs off Main
            combine(
                teaSessionRepository.getAllSessionsFlow(),
                tastingTagRepository.getAllFlow(),
                teaRepository.getAllFlow(),
            ) { sessions, vocabulary, teas ->
                Triple(sessions.usageOf(route.tagId), vocabulary, teas.associateBy { it.id })
            }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _state.update { it.copy(isLoading = false) }
                    messages.failure(e, Res.string.couldnt_load)
                }
                .collect { (usage, vocabulary, teas) ->
                    _state.update {
                        it.copy(
                            tag = vocabulary.find { tag -> tag.id == route.tagId } ?: it.tag,
                            vocabulary = vocabulary,
                            usage = usage,
                            teas = teas,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun onIntent(intent: FlavorDetailIntent) {
        when (intent) {
            is FlavorDetailIntent.RenameClicked -> _state.update { it.copy(dialog = FlavorDialog.Rename) }
            is FlavorDetailIntent.MergeClicked -> _state.update { it.copy(dialog = FlavorDialog.MergePicker) }
            is FlavorDetailIntent.DeleteClicked -> _state.update { it.copy(dialog = FlavorDialog.ConfirmDelete) }
            is FlavorDetailIntent.DialogDismissed -> _state.update { it.copy(dialog = null) }
            is FlavorDetailIntent.RenameConfirmed -> rename(intent.name)
            is FlavorDetailIntent.MergeTargetChosen -> _state.update { state ->
                val target = state.vocabulary.find { it.id == intent.targetId } ?: return
                state.copy(dialog = FlavorDialog.ConfirmMerge(target))
            }
            is FlavorDetailIntent.RemovalConfirmed -> remove()
        }
    }

    private fun rename(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                tastingTagRepository.rename(route.tagId, name)
                _state.update { it.copy(dialog = null) }
            } catch (e: Exception) {
                messages.failure(e, Res.string.couldnt_rename_tag)
            }
            _state.update { it.copy(isSaving = false) }
        }
    }

    private fun remove() {
        val target = (_state.value.dialog as? FlavorDialog.ConfirmMerge)?.target
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, dialog = null) }
            removeTastingTagUseCase(route.tagId, mergeInto = target?.id)
                .onSuccess {
                    _navEvents.send(
                        if (target != null) FlavorDetailNavigationEvent.NavigateToFlavor(target.id)
                        else FlavorDetailNavigationEvent.NavigateBack,
                    )
                }
                .onFailure { e ->
                    messages.failure(e, if (target != null) Res.string.couldnt_merge_tag else Res.string.couldnt_delete_tag)
                }
            _state.update { it.copy(isSaving = false) }
        }
    }
}
