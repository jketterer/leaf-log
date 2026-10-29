package dev.jketterer.leaflog.flow

import androidx.compose.ui.test.hasAnyAncestor
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

/** Teas, tea types and vessels, changed where the user keeps them: Dragon Well, never brewed. */
// The tea and vessel menus and the tea type dialogs don't read strings.xml yet
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

        waitFor("Add tea")
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

    private fun field(label: String) = hasSetTextAction() and hasText(label)

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    private fun type(label: String, text: String) {
        val matcher = field(label)
        waitFor(matcher)
        compose.onNode(matcher).performScrollTo().performTextReplacement(text)
    }
}
