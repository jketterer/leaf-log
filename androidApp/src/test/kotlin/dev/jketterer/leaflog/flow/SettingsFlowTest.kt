package dev.jketterer.leaflog.flow

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.hemisphere_northern
import leaf_log.shared.generated.resources.hemisphere_southern
import leaf_log.shared.generated.resources.settings_title
import org.junit.After
import org.junit.Test
import java.util.Locale

/** Settings that follow the device until the user chooses. */
class SettingsFlowTest : FlowTest() {

    private val deviceLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(deviceLocale)

    @Test
    fun `the hemisphere follows the device region until one is chosen`() {
        val north = text(Res.string.hemisphere_northern)
        val south = text(Res.string.hemisphere_southern)
        region("AU")
        launch()
        click(text(Res.string.settings_title))
        waitFor(hasText(south) and isSelected())

        region("US")
        recreate()
        waitFor(hasText(north) and isSelected())

        click(south)
        waitFor(hasText(south) and isSelected())
        region("GB")
        forceStop()
        click(text(Res.string.settings_title))
        waitFor(hasText(south) and isSelected())
    }

    /** The device's region, as set in system settings, which recreates the Activity. */
    private fun region(code: String) = Locale.setDefault(Locale.Builder().setLanguage("en").setRegion(code).build())
}
