package dev.jketterer.leaflog.flow

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.cancel
import leaf_log.shared.generated.resources.discard
import leaf_log.shared.generated.resources.discard_changes_title
import leaf_log.shared.generated.resources.edit_session
import leaf_log.shared.generated.resources.field_location_optional
import leaf_log.shared.generated.resources.field_tea_quantity_required
import leaf_log.shared.generated.resources.field_temperature_required
import leaf_log.shared.generated.resources.field_vessel_required
import leaf_log.shared.generated.resources.log_session
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.navigate_up
import leaf_log.shared.generated.resources.open_menu
import leaf_log.shared.generated.resources.select_tea
import leaf_log.shared.generated.resources.settings_title
import leaf_log.shared.generated.resources.tab_collection
import leaf_log.shared.generated.resources.tab_history
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

// The edit forms' titles, labels and toolbar icons don't read strings.xml yet
class FormFlowTest : FlowTest() {

    @Before
    fun seed() = runBlocking {
        koin.get<InitializeDefaultsUseCase>()()
        val tea = koin.get<CreateTeaUseCase>()(name = "Dragon Well", teaTypeId = "green").getOrThrow()
        val gaiwan = koin.get<BrewingVesselRepository>().getActiveFlow().first().first { it.name == "Gaiwan" }
        koin.get<StartSessionUseCase>()(
            teaId = tea.id,
            vesselId = gaiwan.id,
            waterType = WaterType.FILTERED,
            brewingTime = 30.seconds,
            temperatureCelsius = 80.0,
            waterQuantityMl = 100.0,
            teaQuantityGrams = 5f,
            location = "Office",
            status = SessionStatus.COMPLETED,
        ).getOrThrow()
        Unit
    }

    @Test
    fun `Edit Tea keeps what was typed through recreation and back asks before discarding it`() {
        launch()
        click(text(Res.string.tab_collection))
        click("Dragon Well")
        assertGuarded(
            open = { click("Edit tea") },
            field = "Tea Name *",
            was = "Dragon Well",
            typed = "Longjing",
            behind = "Edit tea",
        )
    }

    @Test
    fun `Edit Vessel keeps what was typed through recreation and back asks before discarding it`() {
        launch()
        click(text(Res.string.tab_collection))
        click("Vessels")
        click("Gaiwan")
        assertGuarded(
            // Edit does nothing until the vessel loads
            open = {
                waitFor("120 mL")
                click("Edit")
            },
            field = "Name *",
            was = "Gaiwan",
            typed = "Porcelain gaiwan",
            behind = "Edit",
        )
    }

    @Test
    fun `Edit Tea Type keeps what was typed through recreation and back asks before discarding it`() {
        launch()
        click(text(Res.string.settings_title))
        compose.onNode(hasText("Tea Types")).performScrollTo()
        click("Tea Types")
        assertGuarded(
            open = { click("Green") },
            field = "Name",
            was = "Green",
            typed = "Green tea",
            behind = "Add tea type",
        )
    }

    @Test
    fun `Edit Session keeps what was typed through recreation and back asks before discarding it`() {
        launch()
        click(text(Res.string.tab_history))
        click("Dragon Well")
        assertGuarded(
            open = {
                click(text(Res.string.more_options))
                click(text(Res.string.edit_session))
            },
            field = text(Res.string.field_location_optional),
            was = "Office",
            typed = "Garden",
            behind = text(Res.string.more_options),
        )
    }

    @Test
    fun `Log Tea half filled keeps its tea vessel and typing through recreation`() {
        launch()
        click(text(Res.string.open_menu))
        click(text(Res.string.log_session))
        click(text(Res.string.select_tea))
        // Under Recent and under All
        waitFor("Dragon Well")
        compose.onAllNodes(hasClickAction() and hasText("Dragon Well"))[0].performClick()
        click(text(Res.string.field_vessel_required))
        click("Gaiwan")
        type(text(Res.string.field_tea_quantity_required), "4")
        type(text(Res.string.field_temperature_required), "85")

        recreate()

        waitFor(field(text(Res.string.field_tea_quantity_required)) and hasText("4"))
        waitFor(field(text(Res.string.field_temperature_required)) and hasText("85"))
        waitFor("Dragon Well")
        waitFor(hasText(text(Res.string.field_vessel_required)) and hasText("Gaiwan"))
    }

    @Test
    fun `system back from a tab's root returns Home and then leaves the app`() {
        launch()
        click(text(Res.string.tab_history))
        systemBack()
        waitFor(text(Res.string.settings_title))
        assertFalse(isFinishing)

        systemBack()
        assertTrue(isFinishing)
    }

    /**
     * Opens a form with [open], showing [field] as [was], over a screen showing [behind]. Back
     * leaves it at once until [typed] replaces [was]; after that back and the arrow ask first,
     * the typing and the question both outlive recreation, and Discard leaves without saving.
     */
    private fun assertGuarded(open: () -> Unit, field: String, was: String, typed: String, behind: String) {
        open()
        waitFor(field(field) and hasText(was))
        systemBack()
        waitFor(behind)

        open()
        type(field, typed)
        recreate()
        waitFor(field(field) and hasText(typed))

        systemBack()
        waitFor(text(Res.string.discard_changes_title))
        recreate()
        click(inDialog(text(Res.string.cancel)))
        waitFor(field(field) and hasText(typed))

        click(text(Res.string.navigate_up))
        click(inDialog(text(Res.string.discard)))
        waitFor(behind)
        assertAbsent(typed)
    }

    private fun field(label: String) = hasSetTextAction() and hasText(label)

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    private fun type(label: String, text: String) {
        val matcher = field(label)
        waitFor(matcher)
        compose.onNode(matcher).performTextReplacement(text)
    }
}
