package dev.jketterer.leaflog.presentation.ui.screens.sessionreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.toggled
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.configuration.FindMethodUpdateUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.ShouldOfferSaveAsMethodUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.UpdateMethodFromSessionUseCase
import dev.jketterer.leaflog.domain.usecases.session.CompareWithBestUseCase
import dev.jketterer.leaflog.domain.usecases.session.EditSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.VsBest
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_create_tag
import leaf_log.shared.generated.resources.couldnt_load_session
import leaf_log.shared.generated.resources.couldnt_save_changes
import leaf_log.shared.generated.resources.couldnt_save_method
import leaf_log.shared.generated.resources.couldnt_save_photo
import leaf_log.shared.generated.resources.couldnt_update_method
import leaf_log.shared.generated.resources.failure_not_found
import kotlin.uuid.Uuid

class SessionReviewViewModel(
    private val route: NavRoute.SessionReviewRoute,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val tastingTagRepository: TastingTagRepository,
    private val imageStorage: ImageStorage,
    private val editSteepUseCase: EditSteepUseCase,
    private val compareWithBestUseCase: CompareWithBestUseCase,
    private val shouldOfferSaveAsMethodUseCase: ShouldOfferSaveAsMethodUseCase,
    private val generateConfigurationLabelUseCase: GenerateConfigurationLabelUseCase,
    private val saveBrewingConfigurationUseCase: SaveBrewingConfigurationUseCase,
    private val findMethodUpdateUseCase: FindMethodUpdateUseCase,
    private val updateMethodFromSessionUseCase: UpdateMethodFromSessionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionReviewState())
    val state: StateFlow<SessionReviewState> = _state.asStateFlow()

    private val _navigationEvents = Channel<SessionReviewNavEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    val messages = UiMessages<Nothing>()

    init {
        load()
    }

    fun onIntent(intent: SessionReviewIntent) {
        when (intent) {
            is SessionReviewIntent.NotesChanged -> {
                _state.update { it.copy(notes = intent.notes) }
                editSteep(firstSteepId()) { it.copy(notes = _state.value.notes) }
            }

            is SessionReviewIntent.PhotoAdded -> addPhoto(intent.imageBytes)
            is SessionReviewIntent.PhotoRemoved -> removePhoto(intent.path)
            is SessionReviewIntent.SteepClicked -> openSteep(intent.steepId)
            is SessionReviewIntent.SteepRatingChanged -> {
                val id = _state.value.editingSteepId ?: return
                _state.update { it.copy(editRating = intent.rating) }
                editSteep(id) { it.copy(rating = _state.value.editRating.takeIf { r -> r > 0f }) }
            }

            is SessionReviewIntent.SteepNotesChanged -> {
                val id = _state.value.editingSteepId ?: return
                _state.update {
                    it.copy(
                        editNotes = intent.notes,
                        notes = if (id == firstSteepId()) intent.notes else it.notes,
                    )
                }
                editSteep(id) { it.copy(notes = _state.value.editNotes) }
            }

            is SessionReviewIntent.SteepTagToggled -> toggleTag(intent.tagId)
            is SessionReviewIntent.SteepTagCreated -> createTag(intent.name)
            is SessionReviewIntent.DismissSteepEdit -> _state.update { it.copy(editingSteepId = null) }
            is SessionReviewIntent.MethodLabelChanged -> _state.update { it.copy(methodLabel = intent.label) }
            is SessionReviewIntent.SaveMethod -> saveMethod()
            is SessionReviewIntent.UpdateMethod -> updateMethod()
            is SessionReviewIntent.Done -> _navigationEvents.trySend(
                SessionReviewNavEvent.NavigateToHome(
                    route.sessionId
                )
            )
        }
    }

    private fun firstSteepId() = route.sessionId

    private fun load() {
        viewModelScope.launch {
            try {
                val session = teaSessionRepository.getSessionOfSteep(route.sessionId)
                if (session == null) {
                    _state.update { it.copy(isLoading = false) }
                    messages.show(Res.string.failure_not_found)
                    return@launch
                }
                val vsBest = compareWithBestUseCase(session)
                val bestVesselId = (vsBest as? VsBest.Versus)?.best?.session?.vesselId
                val offer = shouldOfferSaveAsMethodUseCase(session)
                val first = session.steeps.first()
                val methodToUpdate = findMethodUpdateUseCase(session)
                _state.update {
                    it.copy(
                        session = session,
                        tea = teaRepository.getById(session.session.teaId),
                        vessel = brewingVesselRepository.getById(session.session.vesselId),
                        bestVessel = bestVesselId?.let { id -> brewingVesselRepository.getById(id) },
                        vsBest = vsBest,
                        notes = first.notes ?: "",
                        offerMethod = offer,
                        methodToUpdate = methodToUpdate,
                        methodLabel = if (offer) suggestedLabel(session) else "",
                        vocabulary = tastingTagRepository.getAll(),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                messages.failure(e, Res.string.couldnt_load_session)
            }
        }
    }

    private fun suggestedLabel(session: SessionWithSteeps): String =
        generateConfigurationLabelUseCase(
            teaQuantityGrams = session.session.teaQuantityGrams,
            waterQuantityMl = session.steeps.first().waterQuantityMl,
            brewingTime = BrewingPlan.learnedFrom(session.steeps).base,
        )

    private fun editSteep(steepId: String, edit: (Steep) -> Steep) {
        viewModelScope.launch {
            editSteepUseCase(steepId, edit)
                .onSuccess { updated -> _state.update { it.copy(session = updated) } }
                .onFailure { messages.failure(it, Res.string.couldnt_save_changes) }
        }
    }

    private fun toggleTag(tagId: String) {
        val id = _state.value.editingSteepId ?: return
        editSteep(id) { it.copy(tags = it.tags.toggled(tagId)) }
    }

    /** Makes the tag and puts it on the steep being edited. */
    private fun createTag(name: String) {
        viewModelScope.launch {
            try {
                val tag = tastingTagRepository.createCustom(name)
                _state.update { it.copy(vocabulary = it.vocabulary + tag) }
                toggleTag(tag.id)
            } catch (e: Exception) {
                messages.failure(e, Res.string.couldnt_create_tag)
            }
        }
    }

    private fun openSteep(steepId: String) {
        val steep = _state.value.session?.steeps?.firstOrNull { it.id == steepId } ?: return
        _state.update {
            it.copy(
                editingSteepId = steepId,
                editRating = steep.rating ?: 0f,
                editNotes = steep.notes ?: ""
            )
        }
    }

    private fun addPhoto(imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                val path =
                    imageStorage.saveImage(imageBytes, "${Uuid.random()}.jpg", "session_images")
                editSteep(firstSteepId()) { it.copy(photos = it.photos + path) }
            } catch (e: Exception) {
                messages.failure(e, Res.string.couldnt_save_photo)
            }
        }
    }

    private fun removePhoto(path: String) {
        editSteep(firstSteepId()) { it.copy(photos = it.photos - path) }
        viewModelScope.launch {
            try {
                imageStorage.deleteImage(path)
            } catch (_: Exception) {
            }
        }
    }

    private fun updateMethod() {
        val state = _state.value
        val session = state.session ?: return
        val method = state.methodToUpdate ?: return
        viewModelScope.launch {
            updateMethodFromSessionUseCase(method.id, session)
                .onSuccess {
                    _state.update {
                        it.copy(
                            methodToUpdate = null,
                            methodUpdated = true
                        )
                    }
                }
                .onFailure { messages.failure(it, Res.string.couldnt_update_method) }
        }
    }

    private fun saveMethod() {
        val state = _state.value
        val session = state.session ?: return
        viewModelScope.launch {
            saveBrewingConfigurationUseCase(session, state.methodLabel.ifBlank { null })
                .onSuccess { _state.update { it.copy(offerMethod = false, methodSaved = true) } }
                .onFailure { messages.failure(it, Res.string.couldnt_save_method) }
        }
    }
}

sealed interface SessionReviewNavEvent {
    data class NavigateToHome(val finishedSessionId: String) : SessionReviewNavEvent
}
