package dev.jketterer.leaflog.flow

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TastingTagRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.CreateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.AddSteepUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.prefill_best_brew
import leaf_log.shared.generated.resources.prefill_saved_method
import leaf_log.shared.generated.resources.review_update_method
import leaf_log.shared.generated.resources.review_save_method
import leaf_log.shared.generated.resources.review_method_updated
import leaf_log.shared.generated.resources.review_method_saved
import leaf_log.shared.generated.resources.review_method_label
import leaf_log.shared.generated.resources.review_how_was_steep_n
import leaf_log.shared.generated.resources.review_finish_session
import leaf_log.shared.generated.resources.review_done
import leaf_log.shared.generated.resources.select_tea
import leaf_log.shared.generated.resources.open_menu
import leaf_log.shared.generated.resources.log_session
import leaf_log.shared.generated.resources.field_vessel_required
import leaf_log.shared.generated.resources.begin_session
import leaf_log.shared.generated.resources.brew_again
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.delete_session
import leaf_log.shared.generated.resources.delete_steep
import leaf_log.shared.generated.resources.edit_parameters
import leaf_log.shared.generated.resources.field_temperature
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.next_session
import leaf_log.shared.generated.resources.history_empty
import leaf_log.shared.generated.resources.previous_session
import leaf_log.shared.generated.resources.review_add_note
import leaf_log.shared.generated.resources.review_note_label
import leaf_log.shared.generated.resources.review_steep_n
import leaf_log.shared.generated.resources.save
import leaf_log.shared.generated.resources.start_brewing
import leaf_log.shared.generated.resources.tab_history
import leaf_log.shared.generated.resources.tag_floral
import leaf_log.shared.generated.resources.tags_add
import leaf_log.shared.generated.resources.tags_create
import leaf_log.shared.generated.resources.tags_search
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * A finished session read back from History, and changed there: Dragon Well in the Gaiwan
 * yesterday, steep 1 at 0:30 and 80° rated 4, steep 2 at 0:45 and 85°.
 */
class SessionFlowTest : FlowTest() {

    private lateinit var sessionId: String

    @Before
    fun seed() = runBlocking {
        koin.get<InitializeDefaultsUseCase>()()
        val tea = koin.get<CreateTeaUseCase>()(name = "Dragon Well", teaTypeId = "green").getOrThrow()
        val gaiwan = koin.get<BrewingVesselRepository>().getActiveFlow().first().first { it.name == "Gaiwan" }
        val today = clock.now()
        clock.set(today - 1.days)
        sessionId = koin.get<StartSessionUseCase>()(
            teaId = tea.id,
            vesselId = gaiwan.id,
            waterType = WaterType.FILTERED,
            brewingTime = 30.seconds,
            temperatureCelsius = 80.0,
            waterQuantityMl = 100.0,
            teaQuantityGrams = 5f,
            rating = 4f,
            status = SessionStatus.COMPLETED,
        ).getOrThrow().session.id
        koin.get<AddSteepUseCase>()(sessionId, brewingTime = 45.seconds, temperatureCelsius = 85.0, waterQuantityMl = 100.0)
            .getOrThrow()
        clock.set(today)
    }

    @Test
    fun `a steep's rating note and tags changed from the session outlast a restart`() {
        openSession()
        click(steep(2))
        rate(3)
        click(text(Res.string.review_add_note))
        type(text(Res.string.review_note_label), "Sweeter cooled")
        click(text(Res.string.tags_add))
        click(text(Res.string.tag_floral))
        type(text(Res.string.tags_search), "Orchid")
        click(text(Res.string.tags_create, "Orchid"))
        dismissSheet()
        dismissSheet()
        click(text(Res.string.navigate_up))
        waitFor(text(Res.string.tab_history))

        forceStop()
        click(text(Res.string.tab_history))
        click(row)
        waitFor(steep(2) and hasText("Sweeter cooled") and hasText(text(Res.string.tag_floral)) and hasText("Orchid"))
        val second = session().steeps[1]
        assertEquals(3f, second.rating)
        assertEquals("Sweeter cooled", second.notes)
        val orchid = runBlocking { koin.get<TastingTagRepository>().getAll() }.single { it.name == "Orchid" }
        assertEquals(setOf("floral", orchid.id), second.tags.filter { it.confirmed }.map { it.tagId }.toSet())
    }

    @Test
    fun `a steep's parameters changed from the session are saved to it alone`() {
        openSession()
        click(steep(2))
        click(text(Res.string.edit_parameters))
        type(text(Res.string.field_temperature), "90")
        click(text(Res.string.save))
        dismissSheet()

        waitFor(steep(2) and hasText("90°C"))
        val (first, second) = session().steeps
        assertEquals(90.0, second.temperatureCelsius, 0.0)
        assertEquals(80.0, first.temperatureCelsius, 0.0)
    }

    @Test
    fun `a later steep deleted after confirming leaves the rest and steep 1 offers no delete`() {
        openSession()
        click(steep(1))
        waitFor(text(Res.string.edit_parameters))
        assertAbsent(text(Res.string.delete_steep))
        dismissSheet()

        click(steep(2))
        click(text(Res.string.delete_steep))
        click(inDialog(text(Res.string.delete)))

        waitUntilGone(text(Res.string.review_steep_n, 2))
        waitFor(steep(1))
        assertEquals(1, session().steeps.size)
    }

    @Test
    fun `a session deleted after confirming leaves History`() {
        openSession()
        click(text(Res.string.more_options))
        click(text(Res.string.delete_session))
        click(inDialog(text(Res.string.delete)))

        waitFor(text(Res.string.history_empty))
        assertEquals(emptyList<SessionWithSteeps>(), runBlocking { koin.get<TeaSessionRepository>().getAllSessions() })
    }

    @Test
    fun `brew again starts a new session set up as this one on the Timer`() {
        openSession()
        click(text(Res.string.brew_again))

        waitFor(text(Res.string.start_brewing))
        waitFor("0:30")
        val sessions = runBlocking { koin.get<TeaSessionRepository>().getAllSessions() }
        val again = sessions.single { it.session.id != sessionId }
        assertEquals(SessionStatus.IN_PROGRESS, again.session.status)
        assertEquals(5f, again.session.teaQuantityGrams)
        assertNull(again.steeps.single().rating)
    }

    @Test
    fun `the arrows step to the next and previous session without leaving`() {
        runBlocking {
            koin.get<StartSessionUseCase>()(session()).getOrThrow()
        }
        // History lists the newest first
        openSession(index = 1)
        click(text(Res.string.next_session))
        waitUntilGone(text(Res.string.review_steep_n, 2))
        waitFor(steep(1))

        click(text(Res.string.previous_session))
        waitFor(steep(2))
    }

    @Test
    fun `a steep rated and noted on the review is saved with its session`() {
        launch()
        brewAndFinish()
        click(steep(1))
        rate(2)
        // The review's own note is labeled the same, so type into the one the sheet adds
        val note = hasSetTextAction() and hasText(text(Res.string.review_note_label))
        val sessionNote = compose.onNode(note).fetchSemanticsNode().id
        click(text(Res.string.review_add_note))
        waitUntil("the steep's note opens") { compose.onAllNodes(note).fetchSemanticsNodes().size == 2 }
        compose.onAllNodes(note).filter(SemanticsMatcher("not the session's") { it.id != sessionNote })
            .onFirst().performTextReplacement("Too hot")
        dismissSheet()
        click(text(Res.string.review_done))

        waitFor(text(Res.string.tab_history))
        val brewed = latest().steeps.single()
        assertEquals(2f, brewed.rating)
        assertEquals("Too hot", brewed.notes)
    }

    @Test
    fun `a method named and saved on the review is kept for its tea and vessel`() {
        launch()
        brewAndFinish()
        compose.onNode(hasText(text(Res.string.review_save_method)) and hasClickAction()).performScrollTo()
        type(text(Res.string.review_method_label), "Morning")
        click(text(Res.string.review_save_method))

        waitFor(text(Res.string.review_method_saved))
        val method = runBlocking { koin.get<BrewingConfigurationRepository>().getAll() }.single()
        assertEquals("Morning", method.label)
        assertEquals(latest().session.vesselId, method.vesselId)
        assertEquals(30.seconds, method.brewingTime)
    }

    @Test
    fun `a method brewed longer than it says is updated from the review`() {
        val method = runBlocking {
            val session = session()
            koin.get<CreateBrewingConfigurationUseCase>()(
                teaId = session.session.teaId,
                vesselId = session.session.vesselId,
                teaQuantityGrams = 5f,
                waterQuantityMl = 100.0,
                temperatureCelsius = 80.0,
                brewingTime = 30.seconds,
                waterType = WaterType.FILTERED,
                customLabel = "Morning",
            ).getOrThrow()
        }
        launch()
        startLogging()
        waitFor(text(Res.string.prefill_saved_method, "Dragon Well"))
        click(text(Res.string.begin_session))
        click(text(Res.string.start_brewing))
        click("+10s")
        clock.advance(40.seconds)
        finish()
        compose.onNode(hasText(text(Res.string.review_update_method)) and hasClickAction()).performScrollTo()
        click(hasText(text(Res.string.review_update_method)) and hasClickAction())

        waitFor(text(Res.string.review_method_updated))
        val updated = runBlocking { koin.get<BrewingConfigurationRepository>().getById(method.id) }!!
        assertEquals(40.seconds, updated.brewingTime)
        assertEquals(method.id, latest().session.usedConfigurationId)
    }

    private val row = hasText("Dragon Well") and hasClickAction()

    private fun openSession(index: Int = 0) {
        launch()
        click(text(Res.string.tab_history))
        waitFor(row)
        compose.onAllNodes(row)[index].performClick()
    }

    /** From Home through Log Tea to the Timer's first steep, Dragon Well in the Gaiwan, not started. */
    private fun startLogging() {
        click(text(Res.string.open_menu))
        click(text(Res.string.log_session))
        click(text(Res.string.select_tea))
        // Under Recent and under All
        waitFor(row)
        compose.onAllNodes(row)[0].performClick()
        click(text(Res.string.field_vessel_required))
        click("Gaiwan")
    }

    /** A new steep set up as the best brew, 0:30, rated 4 and finished, leaving its Session Review open. */
    private fun brewAndFinish() {
        startLogging()
        waitFor(text(Res.string.prefill_best_brew, "Dragon Well", "4"))
        click(text(Res.string.begin_session))
        click(text(Res.string.start_brewing))
        clock.advance(30.seconds)
        finish()
    }

    private fun finish() {
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        rate(4)
        click(text(Res.string.review_finish_session))
        waitFor(text(Res.string.review_done))
    }

    /** The session most recently started. */
    private fun latest() = runBlocking { koin.get<TeaSessionRepository>().getAllSessions() }.maxBy { it.startedAt }

    private fun steep(n: Int) = hasText(text(Res.string.review_steep_n, n)) and hasClickAction()

    private fun session() = runBlocking { koin.get<TeaSessionRepository>().getSession(sessionId)!! }

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    private fun type(label: String, text: String) {
        val matcher = hasSetTextAction() and hasText(label)
        waitFor(matcher)
        compose.onNode(matcher).performTextReplacement(text)
    }

    /** Closes the topmost sheet as TalkBack does; each sheet is its own window and takes back. */
    private fun dismissSheet() {
        val sheet = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
        waitFor(sheet)
        compose.onAllNodes(sheet).onLast().performSemanticsAction(SemanticsActions.Dismiss)
        compose.waitForIdle()
    }
}
