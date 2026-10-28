package dev.jketterer.leaflog.flow

import android.app.Notification
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl.Companion.COMPLETION_NOTIFICATION_ID
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl.Companion.NOTIFICATION_ID
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl.Companion.REMINDER_NOTIFICATION_ID
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.services.SessionReminderReceiver
import dev.jketterer.leaflog.services.TimerCompletionReceiver
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.discard
import leaf_log.shared.generated.resources.discard_session_action
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.pause
import leaf_log.shared.generated.resources.review_done
import leaf_log.shared.generated.resources.review_finish_session
import leaf_log.shared.generated.resources.review_how_was_steep_n
import leaf_log.shared.generated.resources.start_brewing
import leaf_log.shared.generated.resources.start_steep_n
import leaf_log.shared.generated.resources.tab_history
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

// The timer's Resume and the reminder's text don't read strings.xml yet
class NotificationFlowTest : FlowTest() {

    @Before
    fun seed() = runBlocking {
        koin.get<InitializeDefaultsUseCase>()()
        koin.get<CreateTeaUseCase>()(name = "Dragon Well", teaTypeId = "green").getOrThrow()
        koin.get<CreateTeaUseCase>()(name = "Tie Guan Yin", teaTypeId = "oolong").getOrThrow()
        Unit
    }

    @Test
    fun `the ongoing notification names the tea and steep and holds still while paused`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        clock.advance(10.seconds)

        waitUntil("the ongoing notification counts down from 0:20") { ongoing()?.progress == 666 }
        assertEquals("Dragon Well", ongoing()!!.title)
        assertEquals("Steep 1", ongoing()!!.text)
        assert(ongoing()!!.showsChronometer)

        click(text(Res.string.pause))
        waitUntil("the ongoing notification reads paused") { ongoing()?.text == "Steep 1 paused" }
        assertEquals("0:20", ongoing()!!.subText)
        assertFalse(ongoing()!!.showsChronometer)

        click("Resume")
        waitUntil("the ongoing notification counts down again") {
            ongoing()?.text == "Steep 1" && ongoing()!!.showsChronometer
        }
    }

    @Test
    fun `the ready alert names the next steep and goes with the reminder once the review moves on`() {
        launch()
        beginSession("Dragon Well")
        brewSteep(1)
        assertEquals("Dragon Well is ready!", ready()!!.title)
        assertEquals("Steep 1 done, next 30s", ready()!!.text)

        fireAlarm(SessionReminderReceiver::class)

        click(text(Res.string.start_steep_n, 2))
        waitUntil("Next withdraws steep 1's ready alert and reminder") {
            ready() == null && reminder() == null
        }
        brewSteep(2)
        click(text(Res.string.start_steep_n, 3))
        waitUntil("Next withdraws steep 2's ready alert and its scheduled reminder") {
            ready() == null && alarm(SessionReminderReceiver::class) == null
        }
        brewSteep(3)
        fireAlarm(SessionReminderReceiver::class)
        assertEquals("How was your Dragon Well?", reminder()!!.title)

        click(text(Res.string.review_finish_session))
        waitUntil("Finish withdraws the ready alert and the reminder") {
            ready() == null && reminder() == null && alarm(SessionReminderReceiver::class) == null
        }
    }

    @Test
    fun `discarding from the review withdraws the ready alert and the reminder`() {
        launch()
        beginSession("Dragon Well")
        brewSteep(1)
        assertNotNull(alarm(SessionReminderReceiver::class))

        click(text(Res.string.more_options))
        click(text(Res.string.discard_session_action))
        click(hasText(text(Res.string.discard)) and hasAnyAncestor(isDialog()))

        waitUntil("Discard withdraws the ready alert and the reminder") {
            ready() == null && alarm(SessionReminderReceiver::class) == null
        }
    }

    @Test
    fun `with brew complete alerts off a steep completes in the app without one`() {
        runBlocking { koin.get<PreferencesRepository>().updateTimerCompletionNotificationsEnabled(false) }
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        waitUntil("the reminder is scheduled") { alarm(SessionReminderReceiver::class) != null }
        assertNull(alarm(TimerCompletionReceiver::class))

        clock.advance(30.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        assertNull(ready())
    }

    @Test
    fun `with reminders off the ready alert still comes`() {
        runBlocking { koin.get<PreferencesRepository>().updateSessionReminderNotificationsEnabled(false) }
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        waitUntil("the backup alarm is set") { alarm(TimerCompletionReceiver::class) != null }
        assertNull(alarm(SessionReminderReceiver::class))

        clock.advance(30.seconds)
        waitUntil("the ready alert is posted") { ready() != null }
    }

    @Test
    fun `a running session deleted from History takes its alarms with it`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        waitUntil("the alarms and the ongoing notification are set") {
            alarm(TimerCompletionReceiver::class) != null && ongoing() != null
        }

        click(text(Res.string.navigate_up))
        click(text(Res.string.tab_history))
        click(text(Res.string.more_options))
        click(text(Res.string.delete))
        click(hasText(text(Res.string.delete)) and hasAnyAncestor(isDialog()))

        waitUntil("the backup alarm, the reminder and the ongoing notification are gone") {
            alarm(TimerCompletionReceiver::class) == null && alarm(SessionReminderReceiver::class) == null &&
                ongoing() == null
        }
    }

    @Test
    fun `taking 10s off a running steep moves its alarms and countdown and it alerts at the new end`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        waitUntil("the alarms are set") { alarm(SessionReminderReceiver::class) != null }
        val backup = alarm(TimerCompletionReceiver::class)!!.triggerAtTime
        val reminder = alarm(SessionReminderReceiver::class)!!.triggerAtTime
        clock.advance(10.seconds)
        waitFor("0:20")

        click("-10s")
        waitFor("0:10")
        // The alarms' clock stands still, so each comes 20s sooner: the 10s gone and the 10s taken off
        waitUntil("the alarms come 20s sooner") {
            backup - alarm(TimerCompletionReceiver::class)!!.triggerAtTime in 19_000..21_000 &&
                reminder - alarm(SessionReminderReceiver::class)!!.triggerAtTime in 19_000..21_000
        }
        clock.advance(1.seconds)
        waitUntil("the ongoing countdown ends 9s from now") {
            ongoing()!!.`when` - System.currentTimeMillis() in 8_500..9_500
        }

        clock.advance(9.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        waitUntil("the ready alert is posted") { ready() != null }
    }

    @Test
    fun `with notifications denied a steep still completes and allowing them later needs no restart`() {
        allowNotifications(false)
        launch()
        beginSession("Dragon Well")
        waitFor(NOTIFICATIONS_DISABLED)
        click(text(Res.string.start_brewing))
        clock.advance(30.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))

        allowNotifications(true)
        click(text(Res.string.start_steep_n, 2))
        waitFor(text(Res.string.start_brewing))
        assertAbsent(NOTIFICATIONS_DISABLED)
    }

    @Test
    fun `deleting an earlier session keeps the running session's alarms`() {
        launch()
        beginSession("Dragon Well")
        brewSteep(1)
        rate(4)
        click(text(Res.string.review_finish_session))
        click(text(Res.string.review_done))
        beginSession("Tie Guan Yin")
        click(text(Res.string.start_brewing))
        click(text(Res.string.navigate_up))

        click(text(Res.string.tab_history))
        click(
            hasContentDescription(text(Res.string.more_options)) and hasAnyAncestor(hasText("Dragon Well")),
        )
        click(text(Res.string.delete))
        click(hasText(text(Res.string.delete)) and hasAnyAncestor(isDialog()))
        waitUntil("Dragon Well's session is gone") {
            compose.onAllNodes(hasText("Dragon Well")).fetchSemanticsNodes().isEmpty()
        }

        assertNotNull(alarm(TimerCompletionReceiver::class))
        assertNotNull(alarm(SessionReminderReceiver::class))
    }

    @Test
    fun `the ongoing notification opens its Timer from the background`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        click(text(Res.string.navigate_up))
        waitUntil("the ongoing notification is posted") { ongoing() != null }

        background()
        tap(ongoing()!!)
        waitFor(text(Res.string.pause))
    }

    @Test
    fun `a Timer tap after the steep ran out opens its review once`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        waitUntil("the ongoing notification is posted") { ongoing() != null }
        val timerTap = ongoing()!!
        clock.advance(30.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))

        background()
        tap(timerTap)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        assertAbsent(text(Res.string.start_brewing))

        click(text(Res.string.navigate_up))
        waitFor(text(Res.string.tab_history))
        assertAbsent(text(Res.string.review_how_was_steep_n, 1))
    }

    @Test
    fun `steep 2's ready alert opens steep 2's review from the background`() {
        launch()
        beginSession("Dragon Well")
        brewSteep(1)
        rate(4)
        click(text(Res.string.start_steep_n, 2))
        click(text(Res.string.start_brewing))
        click(text(Res.string.navigate_up))

        background()
        clock.advance(30.seconds)
        waitUntil("the ready alert is posted") { ready() != null }
        tap(ready()!!)
        waitFor(text(Res.string.review_how_was_steep_n, 2))
    }

    @Test
    fun `after a kill the backup alarm's alert starts the app on the review and only once`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        clock.advance(10.seconds)

        kill()
        clock.advance(20.seconds)
        fireAlarm(TimerCompletionReceiver::class)
        assertEquals("Dragon Well is ready!", ready()!!.title)
        assertEquals("Steep 1 done, next 30s", ready()!!.text)

        tap(ready()!!)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        click(text(Res.string.navigate_up))
        waitFor(text(Res.string.tab_history))

        recreate()
        waitFor(text(Res.string.tab_history))
        assertAbsent(text(Res.string.review_how_was_steep_n, 1))

        dieInBackground()
        waitFor(text(Res.string.tab_history))
        assertAbsent(text(Res.string.review_how_was_steep_n, 1))
    }

    @Test
    fun `the reminder opens the review of the session it names`() {
        launch()
        beginSession("Dragon Well")
        brewSteep(1)
        click(text(Res.string.navigate_up))
        fireAlarm(SessionReminderReceiver::class)

        background()
        tap(reminder()!!)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
    }

    @Test
    fun `a reminder for a finished session opens Home`() {
        launch()
        beginSession("Dragon Well")
        brewSteep(1)
        fireAlarm(SessionReminderReceiver::class)
        val reminder = reminder()!!
        rate(4)
        click(text(Res.string.review_finish_session))
        click(text(Res.string.review_done))

        background()
        tap(reminder)
        waitFor(text(Res.string.tab_history))
        assertAbsent(text(Res.string.review_how_was_steep_n, 1))
    }

    /** Starts the Timer's steep [n] and lets it run out, to its review with the ready alert up. */
    private fun brewSteep(n: Int) {
        click(text(Res.string.start_brewing))
        clock.advance(30.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, n))
        waitUntil("steep $n's ready alert is posted") { ready()?.text?.startsWith("Steep $n done") == true }
    }

    private fun ongoing() = notification(NOTIFICATION_ID)
    private fun ready() = notification(COMPLETION_NOTIFICATION_ID)
    private fun reminder() = notification(REMINDER_NOTIFICATION_ID)

    private val Notification.title get() = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
    private val Notification.text get() = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    private val Notification.subText get() = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
    private val Notification.progress get() = extras.getInt(Notification.EXTRA_PROGRESS)
    private val Notification.showsChronometer get() = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)

    private companion object {
        const val NOTIFICATIONS_DISABLED =
            "Notifications are disabled, so you won't be alerted when your tea is ready in the background"
    }
}
