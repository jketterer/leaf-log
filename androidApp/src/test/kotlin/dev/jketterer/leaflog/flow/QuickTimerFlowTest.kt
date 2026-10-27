package dev.jketterer.leaflog.flow

import android.app.Notification
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performTextInput
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl.Companion.COMPLETION_NOTIFICATION_ID
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl.Companion.NOTIFICATION_ID
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.services.TimerCompletionReceiver
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.cancel
import leaf_log.shared.generated.resources.finish_early
import leaf_log.shared.generated.resources.just_a_timer
import leaf_log.shared.generated.resources.log_it
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.open_menu
import leaf_log.shared.generated.resources.pause
import leaf_log.shared.generated.resources.quick_timer
import leaf_log.shared.generated.resources.review_how_was_steep_n
import leaf_log.shared.generated.resources.review_next_in
import leaf_log.shared.generated.resources.start_brewing
import leaf_log.shared.generated.resources.stop
import leaf_log.shared.generated.resources.stop_timer_title
import leaf_log.shared.generated.resources.tab_history
import leaf_log.shared.generated.resources.what_are_you_brewing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

// The duration sheet, the timer's Resume and the picker's quick-add don't read strings.xml yet
class QuickTimerFlowTest : FlowTest() {

    @Before
    fun seed() = runBlocking {
        koin.get<InitializeDefaultsUseCase>()()
        koin.get<CreateTeaUseCase>()(name = "Dragon Well", teaTypeId = "green").getOrThrow()
        Unit
    }

    @Test
    fun `done in the background it alerts once and its alert offers Just a timer which records nothing`() {
        launch()
        startQuickTimer("1m")
        waitUntil("the backup alarm is set") { alarm(TimerCompletionReceiver::class) != null }

        background()
        clock.advance(60.seconds)
        waitUntil("the ready alert is posted") { ready() != null }
        assertEquals("Your tea is ready!", ready()!!.title)
        assertEquals("Steep 1 done", ready()!!.text)
        waitUntil("the ongoing notification and the backup alarm are gone") {
            ongoing() == null && alarm(TimerCompletionReceiver::class) == null
        }

        tap(ready()!!)
        click(text(Res.string.just_a_timer))
        waitFor(text(Res.string.tab_history))
        assertAbsent(text(Res.string.just_a_timer))
        runBlocking {
            assertEquals(emptyList<Any>(), koin.get<TeaSessionRepository>().getAllSessions())
            assertNull(koin.get<PreferencesRepository>().getQuickTimer())
        }
    }

    @Test
    fun `the ongoing notification counts down opens the Quick Timer and goes on Finish early`() {
        launch()
        startQuickTimer("1m")
        clock.advance(20.seconds)
        waitUntil("the ongoing notification counts down from 0:40") { ongoing()?.progress == 666 }
        assertEquals("Your tea", ongoing()!!.title)
        assertEquals("Steep 1", ongoing()!!.text)

        background()
        tap(ongoing()!!)
        waitFor(text(Res.string.pause))

        click(text(Res.string.more_options))
        click(text(Res.string.finish_early))
        waitFor(text(Res.string.log_it))
        waitUntil("the ongoing notification and the backup alarm are gone") {
            ongoing() == null && alarm(TimerCompletionReceiver::class) == null
        }
        assertNull(ready())
    }

    @Test
    fun `back by the arrow or system back asks first and Stop withdraws its notifications`() {
        launch()
        startQuickTimer("1m")
        waitUntil("the ongoing notification and the backup alarm are up") {
            ongoing() != null && alarm(TimerCompletionReceiver::class) != null
        }

        click(text(Res.string.navigate_up))
        dismissStopDialog()
        click(text(Res.string.pause))
        systemBack()
        dismissStopDialog()
        click("Resume")
        waitFor(text(Res.string.pause))

        systemBack()
        click(hasText(text(Res.string.stop)) and hasAnyAncestor(isDialog()))
        waitFor(text(Res.string.tab_history))
        waitUntil("the ongoing notification and the backup alarm are gone") {
            ongoing() == null && alarm(TimerCompletionReceiver::class) == null
        }
    }

    @Test
    fun `a running Quick Timer keeps counting through recreation`() {
        launch()
        startQuickTimer("1m")
        clock.advance(20.seconds)
        waitFor("0:40")

        recreate()
        waitFor("0:40")
        waitFor(text(Res.string.pause))

        clock.advance(40.seconds)
        waitFor(text(Res.string.log_it))
        waitUntil("the ready alert is posted") { ready() != null }
    }

    @Test
    fun `three steeps by Next then Log it opens steep 3's review with every steep at its real time`() {
        val start = clock.now()
        launch()
        startQuickTimer("30s")
        clock.advance(30.seconds)
        clock.advance(10.seconds)
        brewNext("35s")
        clock.advance(5.seconds)
        brewNext("40s")

        click(text(Res.string.log_it))
        click("Dragon Well")
        waitFor(text(Res.string.review_how_was_steep_n, 3))

        val session = runBlocking { koin.get<TeaSessionRepository>().getAllSessions() }.single()
        assertEquals(start, session.startedAt)
        assertEquals(
            listOf(start to 30.seconds, start + 40.seconds to 35.seconds, start + 80.seconds to 40.seconds),
            session.steeps.map { it.timestamp to it.actualTime },
        )
        assertEquals(listOf(null, null, null), session.steeps.map { it.rating })
    }

    @Test
    fun `what are you brewing hands the running timer to the tea's session`() {
        launch()
        startQuickTimer("1m")
        clock.advance(20.seconds)

        click(text(Res.string.what_are_you_brewing))
        click("Dragon Well")
        assertTimerHandedTo("Dragon Well")
    }

    @Test
    fun `a tea quick-added from the picker takes the running timer too`() {
        launch()
        startQuickTimer("1m")
        clock.advance(20.seconds)

        click(text(Res.string.what_are_you_brewing))
        click("Add new tea")
        compose.onNode(hasSetTextAction()).performTextInput("Bai Hao")
        click("Green")
        click("Add Tea")
        assertTimerHandedTo("Bai Hao")
    }

    @Test
    fun `two steeps by Next outlive the process and attach to a session`() {
        launch()
        startQuickTimer("30s")
        clock.advance(30.seconds)
        click(text(Res.string.review_next_in, "35s"))
        click(text(Res.string.start_brewing))
        clock.advance(10.seconds)
        // A process can't die before a write a tap set off lands, as one ended at once here can
        waitUntil("steep 2's start is saved") {
            runBlocking { koin.get<PreferencesRepository>().getQuickTimer()?.isRunning == true }
        }

        dieInBackground()
        waitFor("0:25")
        clock.advance(25.seconds)
        click(text(Res.string.log_it))
        click("Dragon Well")
        waitFor(text(Res.string.review_how_was_steep_n, 2))

        val session = runBlocking { koin.get<TeaSessionRepository>().getAllSessions() }.single()
        assertEquals(listOf(30.seconds, 35.seconds), session.steeps.map { it.actualTime })
    }

    /** From Home, through the FAB and the duration sheet's [preset]. */
    private fun startQuickTimer(preset: String) {
        click(text(Res.string.open_menu))
        click(text(Res.string.quick_timer))
        click(preset)
        click("Start Timer")
        waitFor(text(Res.string.pause))
    }

    /** From a done Quick Timer, starts the next steep of [length] and lets it run out. */
    private fun brewNext(length: String) {
        click(text(Res.string.review_next_in, length))
        click(text(Res.string.start_brewing))
        clock.advance(length.removeSuffix("s").toInt().seconds)
        waitFor(text(Res.string.log_it))
    }

    private fun dismissStopDialog() {
        waitFor(text(Res.string.stop_timer_title))
        click(hasText(text(Res.string.cancel)) and hasAnyAncestor(isDialog()))
    }

    /** 20s into a 1m Quick Timer, now [tea]'s session Timer, still counting, and naming it. */
    private fun assertTimerHandedTo(tea: String) {
        waitFor("0:40")
        waitFor(text(Res.string.pause))
        waitUntil("the Quick Timer is gone") {
            compose.onAllNodes(hasText(text(Res.string.what_are_you_brewing))).fetchSemanticsNodes().isEmpty()
        }
        waitUntil("the ongoing notification names $tea") { ongoing()?.title == tea }

        clock.advance(40.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        waitUntil("the ready alert names $tea") { ready()?.title == "$tea is ready!" }
    }

    private fun ongoing() = notification(NOTIFICATION_ID)
    private fun ready() = notification(COMPLETION_NOTIFICATION_ID)

    private val Notification.title get() = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
    private val Notification.text get() = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    private val Notification.progress get() = extras.getInt(Notification.EXTRA_PROGRESS)
}
