package dev.jketterer.leaflog.presentation.ui.viewmodel

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.best
import dev.jketterer.leaflog.domain.models.defaultBrew
import dev.jketterer.leaflog.domain.models.lastCompleted
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_load
import org.jetbrains.compose.resources.StringResource

/**
 * The Brew again sheet: the past [brew] Start replays. Opened from a pair, [last] and [best] are
 * what its switch chooses between, both null when there's one brew to show.
 */
data class BrewAgainSheetState(
    val teaName: String,
    val teaTypeColorHex: String?,
    val vesselName: String,
    val brew: SessionWithSteeps,
    val tagVocabulary: List<TastingTag>,
    val last: SessionWithSteeps? = null,
    val best: SessionWithSteeps? = null,
)

/**
 * Opens the Brew again sheet on one past session or on a pair's default brew, and hands its
 * choice to the host: [onStart] to start it, [onAdjust] to open Log Tea on it. Both get the
 * session to replay, and the host runs them through its in-progress check.
 */
class BrewAgainDelegate<T>(
    private val viewModelScope: CoroutineScope,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val teaTypeRepository: TeaTypeRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val tastingTagRepository: TastingTagRepository,
    private val stateFlow: MutableStateFlow<T>,
    private val getSheetState: (T) -> BrewAgainSheetState?,
    private val setSheetState: (T, BrewAgainSheetState?) -> T,
    private val onFailure: (error: Throwable, couldnt: StringResource) -> Unit,
    private val onStart: (sessionId: String) -> Unit,
    private val onAdjust: (sessionId: String) -> Unit,
) {
    /** Shows [sessionId] as it brewed, with no switch. */
    fun openSession(sessionId: String) = open {
        val session = teaSessionRepository.getSession(sessionId)
            ?: throw DomainFailure.NotFound("session $sessionId")
        sheetState(session.session.teaId, session.session.vesselId, session)
    }

    /** Shows the pair's [defaultBrew], and switches between its last and best when they differ. */
    fun openPair(teaId: String, vesselId: String) = open {
        val sessions = teaSessionRepository.getSessionsByTea(teaId).filter { it.session.vesselId == vesselId }
        val brew = sessions.defaultBrew() ?: throw DomainFailure.NotFound("a brew of tea $teaId")
        val last = sessions.lastCompleted()
        val best = sessions.best()?.takeIf { it.session.id != last?.session?.id }
        sheetState(teaId, vesselId, brew).copy(last = best?.let { last }, best = best)
    }

    /** The switch: shows the pair's last or best. */
    fun select(sessionId: String) = stateFlow.update { state ->
        val sheet = getSheetState(state) ?: return@update state
        val brew = listOfNotNull(sheet.last, sheet.best).firstOrNull { it.session.id == sessionId }
            ?: return@update state
        setSheetState(state, sheet.copy(brew = brew))
    }

    fun start() {
        close()?.let { onStart(it.session.id) }
    }

    fun adjust() {
        close()?.let { onAdjust(it.session.id) }
    }

    fun dismiss() {
        close()
    }

    private fun open(load: suspend () -> BrewAgainSheetState) {
        viewModelScope.launch {
            runCatching { load() }
                .onSuccess { sheet -> stateFlow.update { setSheetState(it, sheet) } }
                .onFailure { onFailure(it, Res.string.couldnt_load) }
        }
    }

    private suspend fun sheetState(teaId: String, vesselId: String, brew: SessionWithSteeps): BrewAgainSheetState {
        val tea = teaRepository.getById(teaId) ?: throw DomainFailure.NotFound("tea $teaId")
        return BrewAgainSheetState(
            teaName = tea.name,
            teaTypeColorHex = teaTypeRepository.getById(tea.teaTypeId)?.colorHex,
            vesselName = brewingVesselRepository.getById(vesselId)?.name
                ?: throw DomainFailure.NotFound("vessel $vesselId"),
            brew = brew,
            tagVocabulary = tastingTagRepository.getAll(),
        )
    }

    /** Hides the sheet, returning the brew it showed. */
    private fun close(): SessionWithSteeps? {
        val brew = getSheetState(stateFlow.value)?.brew
        stateFlow.update { setSheetState(it, null) }
        return brew
    }
}
