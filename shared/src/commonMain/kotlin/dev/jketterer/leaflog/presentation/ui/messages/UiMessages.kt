package dev.jketterer.leaflog.presentation.ui.messages

import co.touchlab.kermit.Logger
import dev.jketterer.leaflog.domain.models.DomainFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.failure_in_use_sessions
import leaf_log.shared.generated.resources.failure_in_use_teas
import leaf_log.shared.generated.resources.failure_io
import leaf_log.shared.generated.resources.failure_last_remaining
import leaf_log.shared.generated.resources.failure_newer_backup
import leaf_log.shared.generated.resources.failure_not_a_backup
import leaf_log.shared.generated.resources.failure_not_found
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.cancellation.CancellationException

/** Copy a ViewModel asks for and the screen resolves, so ViewModels stay locale-free. */
sealed interface UiText {
    data class Plain(val resource: StringResource, val args: List<Any> = emptyList()) : UiText

    data class Plural(
        val resource: PluralStringResource,
        val quantity: Int,
        val args: List<Any> = listOf(quantity),
    ) : UiText
}

suspend fun UiText.resolve(): String = when (this) {
    is UiText.Plain -> getString(resource, *args.toTypedArray())
    is UiText.Plural -> getPluralString(resource, quantity, *args.toTypedArray())
}

/** A snackbar. Tapping its [action] sends [Action.intent] back to the ViewModel. */
data class UiMessage<out I>(
    val text: UiText,
    val action: Action<I>? = null,
) {
    data class Action<out I>(val label: StringResource, val intent: I)
}

/** A [UiMessage] waiting in [UiMessages.pending], told apart from an identical one by [id]. */
data class QueuedMessage<out I>(val id: Long, val message: UiMessage<I>)

/**
 * A ViewModel's snackbars, shown one at a time by [UiMessageEffect]. A message stays pending until
 * the screen reports it [shown], so one cut short by rotation or navigation is shown again.
 */
class UiMessages<I> {
    private var nextId = 0L
    private val _pending = MutableStateFlow<List<QueuedMessage<I>>>(emptyList())
    val pending: StateFlow<List<QueuedMessage<I>>> = _pending.asStateFlow()

    fun show(message: UiMessage<I>) {
        val queued = QueuedMessage(nextId++, message)
        _pending.update { it + queued }
    }

    /** Called once the snackbar is dismissed, times out or has its action tapped. */
    fun shown(id: Long) {
        _pending.update { queue -> queue.filterNot { it.id == id } }
    }

    fun show(text: StringResource, vararg args: Any) = show(UiMessage(UiText.Plain(text, args.toList())))

    /** Reports a failed operation, worded by [failureText]. [couldnt] reads "Couldn't <action>". */
    fun failure(error: Throwable, couldnt: StringResource) {
        // The screen closing isn't a failure
        if (error is CancellationException) return
        Logger.w(error, tag = "UiMessages") { couldnt.key }
        show(UiMessage(failureText(error, couldnt)))
    }
}

/** A [DomainFailure] says why it failed; anything else only that [couldnt] happen. */
internal fun failureText(error: Throwable, couldnt: StringResource): UiText = when (error) {
    is DomainFailure.NotFound -> UiText.Plain(Res.string.failure_not_found)
    is DomainFailure.InUse -> when (error.by) {
        DomainFailure.Dependents.SESSIONS ->
            UiText.Plural(Res.plurals.failure_in_use_sessions, error.count)
        DomainFailure.Dependents.TEAS -> UiText.Plural(Res.plurals.failure_in_use_teas, error.count)
    }
    is DomainFailure.LastRemaining -> UiText.Plain(Res.string.failure_last_remaining)
    is DomainFailure.Io -> UiText.Plain(Res.string.failure_io)
    is DomainFailure.NotABackup -> UiText.Plain(Res.string.failure_not_a_backup)
    is DomainFailure.NewerBackup -> UiText.Plain(Res.string.failure_newer_backup)
    else -> UiText.Plain(couldnt)
}
