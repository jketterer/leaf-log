package dev.jketterer.leaflog.presentation.ui.components.common

import dev.jketterer.leaflog.domain.models.TemperatureUnit.CELSIUS
import dev.jketterer.leaflog.domain.models.TemperatureUnit.FAHRENHEIT
import dev.jketterer.leaflog.domain.models.VolumeUnit.FLUID_OUNCES
import dev.jketterer.leaflog.domain.models.VolumeUnit.MILLILITERS
import dev.jketterer.leaflog.domain.models.WeightUnit.GRAMS
import dev.jketterer.leaflog.domain.models.WeightUnit.OUNCES
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnitInputFieldTest {

    @Test
    fun `flipping units any number of times shows the same value`() {
        var celsius = 85.0
        repeat(5) {
            assertEquals("185", unitValueToText(celsius, FAHRENHEIT))
            assertEquals("85", unitValueToText(celsius, CELSIUS))
            // The field re-renders from the value it holds, never from its own rounded text.
            celsius = typedUnitValue(unitValueToText(celsius, FAHRENHEIT), FAHRENHEIT, opened = 85.0)!!
        }
        assertEquals(85.0, celsius)
    }

    @Test
    fun `93 C shown in F and left as it was reports exactly 93`() {
        assertEquals("199", unitValueToText(93.0, FAHRENHEIT))
        assertEquals(93.0, typedUnitValue("199", FAHRENHEIT, opened = 93.0))
    }

    @Test
    fun `a changed value is converted from the unit it was typed in`() {
        assertEquals(100.0, typedUnitValue("212", FAHRENHEIT, opened = 93.0))
        assertEquals(29.5735 * 6.5, typedUnitValue("6.5", FLUID_OUNCES, opened = null)!!, 1e-9)
    }

    @Test
    fun `an untouched field reads back as the value it was given`() {
        for ((value, unit) in listOf(93.0 to FAHRENHEIT, 296.0 to FLUID_OUNCES, 3.0 to OUNCES)) {
            assertEquals(value, typedUnitValue(unitValueToText(value, unit), unit, opened = value))
        }
    }

    @Test
    fun `an emptied field reports no value`() {
        assertNull(typedUnitValue("", CELSIUS, opened = 80.0))
        assertNull(typedUnitValue(".", OUNCES, opened = 3.0))
    }

    @Test
    fun `each unit is shown to its own precision`() {
        assertEquals("93", unitValueToText(93.4, CELSIUS))
        assertEquals("200", unitValueToText(200.4, MILLILITERS))
        assertEquals("6.8", unitValueToText(200.0, FLUID_OUNCES))
        assertEquals("2.5", unitValueToText(2.5, GRAMS))
        assertEquals("0.09", unitValueToText(2.5, OUNCES))
        assertEquals("7", unitValueToText(7.0, GRAMS))
        assertEquals("", unitValueToText(null, GRAMS))
    }

    @Test
    fun `decimal input is accepted up to the unit's precision`() {
        assertTrue(isUnitInput("0.05", OUNCES))
        assertFalse(isUnitInput("0.055", OUNCES))
        assertTrue(isUnitInput("6.", FLUID_OUNCES))
        assertTrue(isUnitInput("2.5", GRAMS))
        assertFalse(isUnitInput("2.55", GRAMS))
        assertFalse(isUnitInput("85.5", CELSIUS))
        assertFalse(isUnitInput("-5", CELSIUS))
        assertFalse(isUnitInput("abc", MILLILITERS))
        assertTrue(isUnitInput("", MILLILITERS))
    }

    @Test
    fun `a trailing point reads as the number before it`() {
        assertEquals(29.5735 * 6, unitTextToValue("6.", FLUID_OUNCES)!!, 1e-9)
    }
}
