package dev.jketterer.leaflog.presentation.ui.messages

import dev.jketterer.leaflog.domain.models.DomainFailure
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_delete_vessel
import leaf_log.shared.generated.resources.failure_in_use_sessions
import leaf_log.shared.generated.resources.failure_in_use_teas
import leaf_log.shared.generated.resources.failure_io
import leaf_log.shared.generated.resources.failure_last_remaining
import leaf_log.shared.generated.resources.failure_newer_backup
import leaf_log.shared.generated.resources.failure_not_a_backup
import leaf_log.shared.generated.resources.failure_not_found
import okio.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals

class UiMessagesTest {

    private fun text(error: Throwable) = failureText(error, Res.string.couldnt_delete_vessel)

    @Test
    fun `an unexpected failure says only what couldn't happen`() {
        assertEquals(
            UiText.Plain(Res.string.couldnt_delete_vessel),
            text(IllegalStateException("SQLITE_BUSY")),
        )
    }

    @Test
    fun `a typed failure gives its reason instead`() {
        assertEquals(UiText.Plain(Res.string.failure_not_found), text(DomainFailure.NotFound("tea")))
        assertEquals(
            UiText.Plain(Res.string.failure_last_remaining),
            text(DomainFailure.LastRemaining("vessel")),
        )
        assertEquals(UiText.Plain(Res.string.failure_io), text(DomainFailure.Io(IOException())))
        assertEquals(UiText.Plain(Res.string.failure_not_a_backup), text(DomainFailure.NotABackup()))
        assertEquals(
            UiText.Plain(Res.string.failure_newer_backup),
            text(DomainFailure.NewerBackup(3)),
        )
    }

    @Test
    fun `in use counts what it's used by`() {
        assertEquals(
            UiText.Plural(Res.plurals.failure_in_use_sessions, 3),
            text(DomainFailure.InUse(3, DomainFailure.Dependents.SESSIONS)),
        )
        assertEquals(
            UiText.Plural(Res.plurals.failure_in_use_teas, 1),
            text(DomainFailure.InUse(1, DomainFailure.Dependents.TEAS)),
        )
    }

    @Test
    fun `a failure is shown and a cancellation isn't`() {
        val messages = UiMessages<Nothing>()

        messages.failure(CancellationException(), Res.string.couldnt_delete_vessel)
        messages.failure(DomainFailure.NotFound("vessel"), Res.string.couldnt_delete_vessel)

        assertEquals(
            listOf(UiText.Plain(Res.string.failure_not_found)),
            messages.pending.value.map { it.message.text },
        )
    }

    @Test
    fun `a message stays pending until it's shown`() {
        val messages = UiMessages<Nothing>()
        messages.show(Res.string.failure_io)
        messages.show(Res.string.failure_io)
        val (first, second) = messages.pending.value

        messages.shown(first.id)

        assertEquals(listOf(second), messages.pending.value)
    }
}
