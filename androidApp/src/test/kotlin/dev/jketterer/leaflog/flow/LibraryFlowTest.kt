package dev.jketterer.leaflog.flow

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.CreateBrewingConfigurationUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.add_method
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.edit
import leaf_log.shared.generated.resources.field_capacity
import leaf_log.shared.generated.resources.field_temperature_required
import leaf_log.shared.generated.resources.field_water_quantity_required
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.no_saved_methods
import leaf_log.shared.generated.resources.save
import leaf_log.shared.generated.resources.settings_title
import leaf_log.shared.generated.resources.tab_collection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Teas' methods, tea types and vessels, changed where the user keeps them: Dragon Well, never brewed. */
// The method form, tea and vessel menus and the tea type dialogs don't read strings.xml yet
class LibraryFlowTest : FlowTest() {

    private lateinit var teaId: String

    @Before
    fun seed() = runBlocking {
        koin.get<InitializeDefaultsUseCase>()()
        teaId = koin.get<CreateTeaUseCase>()(name = "Dragon Well", teaTypeId = "green").getOrThrow().id
    }

    @Test
    fun `a method added from a tea is saved to it with what was entered`() {
        openTea()
        click(text(Res.string.add_method))
        type("Name (optional)", "Morning")
        click("0:30")
        type(text(Res.string.field_temperature_required), "85")
        type(text(Res.string.field_water_quantity_required), "100")
        click(text(Res.string.save))

        // The form's own name field reads Morning too
        waitFor(text(Res.string.add_method))
        waitFor("Morning")
        val method = methods().single()
        assertEquals(teaId, method.teaId)
        assertEquals("Morning", method.label)
        assertEquals(30.seconds, method.brewingTime)
        assertEquals(85.0, method.temperatureCelsius, 0.0)
        assertEquals(100.0, method.waterQuantityMl, 0.0)
    }

    @Test
    fun `a method edited from its tea keeps its identity and takes the change`() {
        val method = seedMethod()
        openTea()
        click(onMethod(text(Res.string.more_options)))
        click(text(Res.string.edit))
        waitFor(field(text(Res.string.field_temperature_required)) and hasText("85"))
        type(text(Res.string.field_temperature_required), "90")
        click(text(Res.string.save))

        waitFor(text(Res.string.add_method))
        val edited = methods().single()
        assertEquals(method.id, edited.id)
        assertEquals(90.0, edited.temperatureCelsius, 0.0)
    }

    @Test
    fun `a method deleted after confirming leaves its tea`() {
        seedMethod()
        openTea()
        click(onMethod(text(Res.string.more_options)))
        click(text(Res.string.delete))
        click(inDialog(text(Res.string.delete)))

        waitFor(text(Res.string.no_saved_methods))
        assertEquals(emptyList<BrewingConfiguration>(), methods())
    }

    @Test
    fun `a tea never brewed is deleted after confirming and leaves the collection`() {
        openTea()
        click("More")
        click("Delete Tea")
        click(inDialog(text(Res.string.delete)))

        waitFor("Add tea")
        waitUntilGone("Dragon Well")
        assertTrue(runBlocking { koin.get<TeaRepository>().getAll() }.none { it.id == teaId })
    }

    @Test
    fun `brew this tea opens Log Tea with the tea chosen`() {
        openTea()
        click("Brew This Tea")

        waitFor("Log Tea Session")
        waitFor(hasText("Dragon Well") and hasText("Green"))
    }

    @Test
    fun `a tea type renamed is saved under its new name`() {
        openTeaTypes()
        click("Green")
        waitFor(field("Name") and hasText("Green"))
        type("Name", "Green tea")
        click("Save")

        waitFor("Green tea")
        assertEquals("Green tea", runBlocking { koin.get<TeaTypeRepository>().getById("green") }!!.name)
    }

    @Test
    fun `a tea type no tea uses is deleted after confirming`() {
        openTeaTypes()
        click("Oolong")
        click("Delete")
        click(inDialog("Delete"))

        waitUntilGone("Oolong")
        waitFor("Green")
        assertEquals(null, runBlocking { koin.get<TeaTypeRepository>().getById("oolong") })
    }

    @Test
    fun `a tea type in use is merged into the one chosen and its teas move with it`() {
        openTeaTypes()
        click("Green")
        click("Delete")
        click(inDialog("Oolong"))
        click(inDialog("Merge"))

        waitUntilGone("Green")
        waitFor("Oolong")
        assertEquals("oolong", runBlocking { koin.get<TeaRepository>().getById(teaId) }!!.teaTypeId)
    }

    @Test
    fun `a vessel added is listed among the active vessels`() {
        openVessels()
        click("Add vessel")
        type("Name *", "Shiboridashi")
        type(text(Res.string.field_capacity), "250")
        click(text(Res.string.save))

        waitFor("Shiboridashi")
        val shiboridashi = vessels().single { it.name == "Shiboridashi" }
        assertEquals(250, shiboridashi.capacityMl)
        assertFalse(shiboridashi.isArchived)
    }

    @Test
    fun `a vessel archived after confirming shows only under All`() {
        openVessels()
        click("Gaiwan")
        waitFor("120 mL")
        click("More options")
        click("Archive")
        click(inDialog("Archive"))

        waitFor("Active")
        waitUntilGone("Gaiwan")
        click("All")
        waitFor("Gaiwan")
        assertTrue(vessels().single { it.name == "Gaiwan" }.isArchived)
    }

    @Test
    fun `a vessel no session used is deleted after confirming`() {
        openVessels()
        click("Gaiwan")
        waitFor("120 mL")
        click("More options")
        click("Delete")
        click(inDialog(text(Res.string.delete)))

        waitFor("Active")
        waitUntilGone("Gaiwan")
        assertTrue(vessels().none { it.name == "Gaiwan" })
    }

    private fun seedMethod() = runBlocking {
        val gaiwan = koin.get<BrewingVesselRepository>().getActiveFlow().first().first { it.name == "Gaiwan" }
        koin.get<CreateBrewingConfigurationUseCase>()(
            teaId = teaId,
            vesselId = gaiwan.id,
            teaQuantityGrams = 5f,
            waterQuantityMl = 100.0,
            temperatureCelsius = 85.0,
            brewingTime = 30.seconds,
            waterType = WaterType.FILTERED,
            customLabel = "Morning",
        ).getOrThrow()
    }

    private fun methods() = runBlocking { koin.get<BrewingConfigurationRepository>().getAll() }

    private fun vessels() = runBlocking { koin.get<BrewingVesselRepository>().getAll() }

    private fun openTea() {
        launch()
        click(text(Res.string.tab_collection))
        click("Dragon Well")
    }

    private fun openTeaTypes() {
        launch()
        click(text(Res.string.settings_title))
        compose.onNode(hasText("Tea Types")).performScrollTo()
        click("Tea Types")
    }

    private fun openVessels() {
        launch()
        click(text(Res.string.tab_collection))
        click("Vessels")
    }

    /** [text] on the Morning method's card. */
    private fun onMethod(text: String) =
        (hasText(text) or androidx.compose.ui.test.hasContentDescription(text)) and
            hasAnyAncestor(androidx.compose.ui.test.hasAnyDescendant(hasText("Morning")))

    private fun field(label: String) = hasSetTextAction() and hasText(label)

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    private fun type(label: String, text: String) {
        val matcher = field(label)
        waitFor(matcher)
        compose.onNode(matcher).performScrollTo().performTextReplacement(text)
    }
}
