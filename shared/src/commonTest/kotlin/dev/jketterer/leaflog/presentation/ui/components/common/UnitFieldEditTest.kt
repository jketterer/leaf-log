package dev.jketterer.leaflog.presentation.ui.components.common

import dev.jketterer.leaflog.domain.models.TemperatureUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UnitFieldEditTest {

    private val fahrenheit = TemperatureUnit.FAHRENHEIT

    private fun edited(text: String, storedCelsius: Double) =
        editedUnitValue(text, storedCelsius, fahrenheit::fromCelsius, fahrenheit::toCelsius)

    @Test
    fun `an untouched field leaves the stored value alone`() {
        assertNull(edited("199", storedCelsius = 93.0))
    }

    @Test
    fun `a changed field is converted to stored units`() {
        assertEquals(100.0, edited("212", storedCelsius = 93.0))
    }

    @Test
    fun `a field that isn't a number leaves the stored value alone`() {
        assertNull(edited("", storedCelsius = 93.0))
        assertNull(edited("hot", storedCelsius = 93.0))
    }
}
