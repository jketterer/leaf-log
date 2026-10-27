package dev.jketterer.leaflog.flow

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.rating_out_of_five
import leaf_log.shared.generated.resources.review_done
import leaf_log.shared.generated.resources.review_finish_session
import leaf_log.shared.generated.resources.review_how_was_steep_n
import leaf_log.shared.generated.resources.session_finished
import leaf_log.shared.generated.resources.settings_title
import leaf_log.shared.generated.resources.start_brewing
import leaf_log.shared.generated.resources.start_steep_n
import leaf_log.shared.generated.resources.steep_count
import leaf_log.shared.generated.resources.tab_history
import leaf_log.shared.generated.resources.undo
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
}
