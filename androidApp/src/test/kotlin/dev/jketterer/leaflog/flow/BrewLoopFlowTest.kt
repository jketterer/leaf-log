package dev.jketterer.leaflog.flow

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.cancel
import leaf_log.shared.generated.resources.discard
import leaf_log.shared.generated.resources.discard_changes_title
import leaf_log.shared.generated.resources.edit_parameters
import leaf_log.shared.generated.resources.field_water_quantity
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.rating_out_of_five
import leaf_log.shared.generated.resources.review_done
import leaf_log.shared.generated.resources.review_finish_session
import leaf_log.shared.generated.resources.review_how_was_steep_n
import leaf_log.shared.generated.resources.save
import leaf_log.shared.generated.resources.session_finished
import leaf_log.shared.generated.resources.settings_title
import leaf_log.shared.generated.resources.start_brewing
import leaf_log.shared.generated.resources.start_steep_n
import leaf_log.shared.generated.resources.steep_count
import leaf_log.shared.generated.resources.tab_history
import leaf_log.shared.generated.resources.undo
import leaf_log.shared.generated.resources.water_type_spring
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class BrewLoopFlowTest : FlowTest() {

    @Before
    fun seed() = runBlocking {
        koin.get<InitializeDefaultsUseCase>()()
        koin.get<CreateTeaUseCase>()(name = "Dragon Well", teaTypeId = "green").getOrThrow()
        Unit
    }

    @Test
    fun `a session brewed through two rated steeps reaches History`() {
        launch()
        brewTwoSteepsAndFinish()

        click(text(Res.string.review_done))
        click(text(Res.string.tab_history))
        waitFor(
            hasText("Dragon Well") and
                hasText(text(Res.plurals.steep_count, 2, 2)) and
                hasContentDescription(text(Res.string.rating_out_of_five, "4.5"), substring = true),
        )
    }

    @Test
    fun `finishing offers Undo once and Undo reopens the last steep`() {
        launch()
        brewTwoSteepsAndFinish()
        click(text(Res.string.review_done))

        click(text(Res.string.undo))
        waitFor(text(Res.string.review_how_was_steep_n, 2))

        click(text(Res.string.review_finish_session))
        click(text(Res.string.review_done))
        waitFor(text(Res.string.session_finished))
        // Past the snackbar's long duration: one cut short by leaving would rightly come back
        compose.mainClock.advanceTimeBy(11_000)
        assertAbsent(text(Res.string.undo))

        click(text(Res.string.settings_title))
        click(text(Res.string.navigate_up))
        waitFor(text(Res.string.tab_history))
        assertAbsent(text(Res.string.undo))

        recreate()
        waitFor(text(Res.string.tab_history))
        assertAbsent(text(Res.string.undo))
    }

    @Test
    fun `parameters changed mid steep ask before a dismiss and stay as they were once discarded`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        val before = session().currentSteep.waterQuantityMl
        click(text(Res.string.edit_parameters))
        val water = hasSetTextAction() and hasText(text(Res.string.field_water_quantity))
        waitFor(water)
        compose.onNode(water).performTextReplacement("150")

        dismissSheet()
        click(inDialog(text(Res.string.cancel)))
        waitFor(water and hasText("150"))

        dismissSheet()
        click(inDialog(text(Res.string.discard)))
        waitUntilGone(text(Res.string.discard_changes_title))
        assertAbsent("150 mL")
        assertEquals(before, session().currentSteep.waterQuantityMl, 0.0)
    }

    @Test
    fun `a water type picked mid steep is saved with the session`() {
        launch()
        beginSession("Dragon Well")
        click(text(Res.string.start_brewing))
        click(text(Res.string.edit_parameters))

        click(text(Res.string.water_type_spring))
        click(text(Res.string.save))

        waitUntil("the session holds spring water") { session().session.waterType == WaterType.SPRING }
    }

    /** From Home to Session Review: steep 1 rated 4 and steep 2 rated 5. */
    private fun brewTwoSteepsAndFinish() {
        beginSession("Dragon Well")

        click(text(Res.string.start_brewing))
        clock.advance(30.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        rate(4)
        click(text(Res.string.start_steep_n, 2))

        click(text(Res.string.start_brewing))
        clock.advance(30.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 2))
        rate(5)
        click(text(Res.string.review_finish_session))
    }

    private fun session() = runBlocking { koin.get<TeaSessionRepository>().getAllSessions().single() }

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    /** Closes the topmost sheet as TalkBack does; each sheet is its own window and takes back. */
    private fun dismissSheet() {
        val sheet = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
        waitFor(sheet)
        compose.onAllNodes(sheet).onLast().performSemanticsAction(SemanticsActions.Dismiss)
        compose.waitForIdle()
    }
}
