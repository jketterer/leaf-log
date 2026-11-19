package dev.jketterer.leaflog.flow

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performClick
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import leaf_log.shared.generated.resources.archive
import leaf_log.shared.generated.resources.brew_with_vessel
import leaf_log.shared.generated.resources.edit_vessel
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.unarchive
import leaf_log.shared.generated.resources.undo
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.delete
import leaf_log.shared.generated.resources.change
import leaf_log.shared.generated.resources.field_capacity
import leaf_log.shared.generated.resources.save
import leaf_log.shared.generated.resources.settings_title
import leaf_log.shared.generated.resources.tab_collection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import leaf_log.shared.generated.resources.open_menu
import leaf_log.shared.generated.resources.add_tea
import leaf_log.shared.generated.resources.collection_tab_flavors
import leaf_log.shared.generated.resources.collection_empty
import leaf_log.shared.generated.resources.add_vessel

/** Teas, tea types and vessels, changed where the user keeps them: Dragon Well, never brewed. */
// The tea menu and the tea type dialogs don't read strings.xml yet
class LibraryFlowTest : FlowTest() {

    private lateinit var teaId: String

    @Before
    fun seed() = runBlocking {
        koin.get<InitializeDefaultsUseCase>()()
        teaId = koin.get<CreateTeaUseCase>()(name = "Dragon Well", teaTypeId = "green").getOrThrow().id
    }

    @Test
    fun `a tea never brewed is deleted after confirming and leaves the collection`() {
        openTea()
        click("More")
        click("Delete Tea")
        click(inDialog(text(Res.string.delete)))

        waitFor(text(Res.string.collection_empty))
        waitUntilGone("Dragon Well")
        assertTrue(runBlocking { koin.get<TeaRepository>().getAll() }.none { it.id == teaId })
    }

    @Test
    fun `brew this tea opens Log Tea with the tea chosen`() {
        openTea()
        click("Brew This Tea")

        waitFor("Log Tea Session")
        waitFor("Dragon Well")
        // Change only shows once a tea is chosen
        waitFor(text(Res.string.change))
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
    fun `Collection's menu adds a tea from any tab`() {
        launch()
        click(text(Res.string.tab_collection))
        click(text(Res.string.collection_tab_flavors))
        click(text(Res.string.open_menu))
        click(text(Res.string.add_tea))

        waitFor("Add Tea")
    }

    @Test
    fun `a vessel added is listed among the active vessels`() {
        openVessels()
        click(text(Res.string.open_menu))
        click(text(Res.string.add_vessel))
        type("Name *", "Shiboridashi")
        type(text(Res.string.field_capacity), "250")
        click(text(Res.string.save))

        waitFor("Shiboridashi")
        val shiboridashi = vessels().single { it.name == "Shiboridashi" }
        assertEquals(250, shiboridashi.capacityMl)
        assertFalse(shiboridashi.isArchived)
    }

    @Test
    fun `a vessel archived from its page stays open and shows only under All`() {
        openGaiwan()
        click(text(Res.string.more_options))
        click(text(Res.string.archive))

        waitFor(text(Res.string.unarchive))
        assertTrue(vessels().single { it.name == "Gaiwan" }.isArchived)
        systemBack()
        waitFor("Active")
        waitUntilGone("Gaiwan")
        click("All")
        waitFor("Gaiwan")
    }

    @Test
    fun `Undo on the archived snackbar brings the vessel back`() {
        openGaiwan()
        click(text(Res.string.more_options))
        click(text(Res.string.archive))
        click(text(Res.string.undo))

        waitFor(text(Res.string.brew_with_vessel))
        assertFalse(vessels().single { it.name == "Gaiwan" }.isArchived)
    }

    @Test
    fun `a vessel edited from its page shows the change on return`() {
        openGaiwan()
        click(text(Res.string.edit_vessel))
        type("Name *", "Old Gaiwan")
        click(text(Res.string.save))

        waitFor(text(Res.string.brew_with_vessel))
        waitFor(hasText("Old Gaiwan") and !hasSetTextAction())
    }

    @Test
    fun `brew with this vessel opens Log Tea on the tea picker with the vessel chosen`() {
        openGaiwan()
        click(text(Res.string.brew_with_vessel))

        val row = hasText("Dragon Well") and hasClickAction()
        waitFor(row)
        compose.onAllNodes(row)[0].performClick()
        waitFor(isSelected() and hasText("Gaiwan"))
    }

    @Test
    fun `a tea brewed in a vessel is listed on its page and opens the tea`() {
        runBlocking {
            val gaiwan = vessels().single { it.name == "Gaiwan" }
            koin.get<StartSessionUseCase>()(
                teaId = teaId,
                vesselId = gaiwan.id,
                waterType = WaterType.FILTERED,
                brewingTime = 30.seconds,
                temperatureCelsius = 80.0,
                waterQuantityMl = 100.0,
                status = SessionStatus.COMPLETED,
            ).getOrThrow()
        }
        openGaiwan()

        waitFor(hasText("1 session, last", substring = true))
        click("Dragon Well")
        waitFor("Brew This Tea")
    }

    @Test
    fun `a vessel no session used is deleted after confirming`() {
        openGaiwan()
        click(text(Res.string.more_options))
        click(text(Res.string.delete))
        click(inDialog(text(Res.string.delete)))

        waitFor("Active")
        waitUntilGone("Gaiwan")
        assertTrue(vessels().none { it.name == "Gaiwan" })
    }

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

    private fun openGaiwan() {
        openVessels()
        click("Gaiwan")
        waitFor("120 mL")
    }

    private fun openVessels() {
        launch()
        click(text(Res.string.tab_collection))
        click("Vessels")
    }

    private fun field(label: String) = hasSetTextAction() and hasText(label)

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    private fun type(label: String, text: String) {
        val matcher = field(label)
        waitFor(matcher)
        compose.onNode(matcher).performScrollTo().performTextReplacement(text)
    }
}
