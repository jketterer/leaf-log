package dev.jketterer.leaflog.flow

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl.Companion.COMPLETION_NOTIFICATION_ID
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.services.SessionReminderReceiver
import dev.jketterer.leaflog.services.TimerCompletionReceiver
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.edit_parameters
import leaf_log.shared.generated.resources.field_water_quantity
import leaf_log.shared.generated.resources.log_a_brew
import leaf_log.shared.generated.resources.log_it
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.open_menu
import leaf_log.shared.generated.resources.pause
import leaf_log.shared.generated.resources.quick_timer
import leaf_log.shared.generated.resources.review_done
import leaf_log.shared.generated.resources.review_finish_session
import leaf_log.shared.generated.resources.review_how_was_steep_n
import leaf_log.shared.generated.resources.save
import leaf_log.shared.generated.resources.session_finished
import leaf_log.shared.generated.resources.start_brewing
import leaf_log.shared.generated.resources.start_steep_n
import leaf_log.shared.generated.resources.tab_collection
import leaf_log.shared.generated.resources.tab_history
import leaf_log.shared.generated.resources.undo
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// The in-progress banner, the timer's Reset and the Quick Timer sheet don't read strings.xml yet
class ProcessDeathFlowTest : FlowTest() {

    @Before
    fun seed() = runBlocking {
        koin.get<InitializeDefaultsUseCase>()()
        koin.get<CreateTeaUseCase>()(name = "Dragon Well", teaTypeId = "green").getOrThrow()
        Unit
    }

    @Test
    fun `steep 2 brewing through a force stop resumes with the time it lost`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        clock.advance(30.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        rate(4)
        click(text(Res.string.start_steep_n, 2))
        click(text(Res.string.start_brewing))
        clock.advance(10.seconds)

        forceStop()
        clock.advance(5.seconds)

        waitFor("Steep 2")
        waitFor("Brewing...")
        click("Resume")
        waitFor("0:15")
        waitFor(text(Res.string.pause))
    }

    @Test
    fun `parameters edited mid steep outlive a force stop with the steep still running`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        clock.advance(10.seconds)
        click(text(Res.string.edit_parameters))
        val water = hasSetTextAction() and hasText(text(Res.string.field_water_quantity))
        waitFor(water)
        compose.onNode(water).performTextReplacement("150")
        click(text(Res.string.save))
        waitFor("150 mL")
        click(text(Res.string.navigate_up))
        waitUntil("the Home banner shows a third gone") { bannerProgress()?.let { abs(it - 1 / 3f) < 0.01f } == true }

        forceStop()
        clock.advance(5.seconds)

        waitUntil("the Home banner shows half gone") { bannerProgress()?.let { abs(it - 0.5f) < 0.01f } == true }
        click("Resume")
        waitFor("0:15")
        waitFor(text(Res.string.pause))
        waitFor("150 mL")
    }

    @Test
    fun `a running steep keeps counting through recreation and alerts only once`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        clock.advance(10.seconds)
        waitFor("0:20")

        recreate()
        waitFor("0:20")
        waitFor(text(Res.string.pause))

        clock.advance(20.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        waitUntil("the ready alert is posted") { notification(COMPLETION_NOTIFICATION_ID) != null }
        // Swiped away, so posting it again would show
        notificationManager.cancel(COMPLETION_NOTIFICATION_ID)

        recreate()
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        assertNull(notification(COMPLETION_NOTIFICATION_ID))
    }

    @Test
    fun `a steep counts on in the background and is done on return once its time has passed`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        background()
        clock.advance(10.seconds)
        foreground()
        waitFor("0:20")

        background()
        clock.advance(30.seconds)
        foreground()
        waitFor(text(Res.string.review_how_was_steep_n, 1))
    }

    @Test
    fun `a paused steep holds through the background and nothing fires at its old end`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        waitUntil("the alarms are set") { alarm(TimerCompletionReceiver::class) != null }
        clock.advance(10.seconds)
        click(text(Res.string.pause))
        waitUntil("pausing cancels the alarms") {
            alarm(TimerCompletionReceiver::class) == null && alarm(SessionReminderReceiver::class) == null
        }

        background()
        clock.advance(1.minutes)
        foreground()
        waitFor("0:20")
        waitFor("Resume")
        assertAbsent(text(Res.string.review_how_was_steep_n, 1))
        assertNull(notification(COMPLETION_NOTIFICATION_ID))
        assertNull(alarm(TimerCompletionReceiver::class))
    }

    @Test
    fun `a steep that ran out while the app was dead opens its review`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        clock.advance(10.seconds)

        forceStop()
        clock.advance(30.seconds)

        waitFor("Ready to finish")
        click("Finish")
        waitFor(text(Res.string.review_how_was_steep_n, 1))
    }

    @Test
    fun `a reset timer stays reset after leaving and after a force stop`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        clock.advance(10.seconds)
        click(text(Res.string.pause))
        click("Reset")
        click(hasText("Reset") and hasAnyAncestor(isDialog()))
        waitFor(text(Res.string.start_brewing))

        click(text(Res.string.navigate_up))
        click("Resume")
        waitFor("0:30")
        waitFor(text(Res.string.start_brewing))

        forceStop()
        click("Resume")
        waitFor("0:30")
        waitFor(text(Res.string.start_brewing))
    }

    @Test
    fun `tabs their screens and the History search come back after recreation and process death`() {
        launch()
        click(text(Res.string.tab_collection))
        click("Dragon Well")
        waitFor(text(Res.string.log_a_brew))
        click(text(Res.string.tab_history))
        click("Search")
        compose.onNode(hasSetTextAction()).performTextInput("Dragon")

        recreate()
        assertTabsAndSearchCameBack()

        dieInBackground()
        assertTabsAndSearchCameBack()
    }

    /** On History searching "Dragon", with Dragon Well's detail open in Collection. */
    private fun assertTabsAndSearchCameBack() {
        waitFor(hasSetTextAction() and hasText("Dragon"))
        click(text(Res.string.tab_collection))
        waitFor(text(Res.string.log_a_brew))
        click(text(Res.string.tab_history))
    }

    @Test
    fun `the Quick Timer comes back counting after the process dies and done once its time passes`() {
        launch()
        click(text(Res.string.open_menu))
        click(text(Res.string.quick_timer))
        click("1m")
        click("Start Timer")
        clock.advance(20.seconds)
        waitFor("0:40")

        dieInBackground()
        waitFor("0:40")
        waitFor(text(Res.string.pause))

        dieInBackground()
        clock.advance(60.seconds)
        waitFor(text(Res.string.log_it))
    }

    @Test
    fun `a Home restored after the process dies does not offer Undo again`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        clock.advance(30.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        rate(4)
        click(text(Res.string.review_finish_session))
        click(text(Res.string.review_done))
        waitFor(text(Res.string.undo))

        dieInBackground()

        waitFor(text(Res.string.tab_history))
        assertAbsent(text(Res.string.undo))
        assertAbsent(text(Res.string.session_finished))
    }

    /** How far the Home banner's progress bar has filled, 0 to 1, or null if it shows none. */
    private fun bannerProgress(): Float? = compose
        .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        .fetchSemanticsNodes()
        .singleOrNull()
        ?.config
        ?.get(SemanticsProperties.ProgressBarRangeInfo)
        ?.current
}
