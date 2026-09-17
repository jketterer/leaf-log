package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.TemperatureUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EditTeaFormLogicTest {

    @Test
    fun blankNameIsRejected() {
        assertNotNull(validateTeaName(""))
        assertNotNull(validateTeaName("   "))
    }

    @Test
    fun anyNonBlankNameIsAccepted() {
        assertNull(validateTeaName("Dragon Well"))
    }

    // The bug this file exists for: temperature is optional, so clearing the field must not block
    // saving.
    @Test
    fun blankTemperatureIsAccepted() {
        assertNull(validateTemperature("", TemperatureUnit.CELSIUS))
        assertNull(validateTemperature("   ", TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun nonNumericTemperatureIsRejected() {
        assertNotNull(validateTemperature("hot", TemperatureUnit.CELSIUS))
        assertNotNull(validateTemperature("80.5", TemperatureUnit.CELSIUS))
    }

    @Test
    fun celsiusAcceptsItsRangeInclusive() {
        assertNull(validateTemperature("0", TemperatureUnit.CELSIUS))
        assertNull(validateTemperature("80", TemperatureUnit.CELSIUS))
        assertNull(validateTemperature("100", TemperatureUnit.CELSIUS))
    }

    @Test
    fun celsiusRejectsOutsideItsRange() {
        assertNotNull(validateTemperature("-1", TemperatureUnit.CELSIUS))
        assertNotNull(validateTemperature("101", TemperatureUnit.CELSIUS))
    }

    @Test
    fun fahrenheitAcceptsItsRangeInclusive() {
        assertNull(validateTemperature("32", TemperatureUnit.FAHRENHEIT))
        assertNull(validateTemperature("212", TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun fahrenheitRejectsOutsideItsRange() {
        assertNotNull(validateTemperature("31", TemperatureUnit.FAHRENHEIT))
        assertNotNull(validateTemperature("213", TemperatureUnit.FAHRENHEIT))
    }

    // 80 is a fine Celsius brew and a freezing Fahrenheit one - the unit has to reach the check.
    @Test
    fun theSameNumberIsJudgedAgainstTheUnitItWasTypedIn() {
        assertNull(validateTemperature("80", TemperatureUnit.CELSIUS))
        assertNotNull(validateTemperature("120", TemperatureUnit.CELSIUS))
        assertNull(validateTemperature("120", TemperatureUnit.FAHRENHEIT))
    }

    // --- temperature seeding -------------------------------------------------------------------

    @Test
    fun anEmptyFieldTakesTheTypeDefault() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = "",
            currentFromTeaTypeId = null,
            newTeaTypeId = "green",
            newTypeDefaultCelsius = 75.0,
        )
        assertEquals("75.0", seed.celsius)
        assertEquals("green", seed.fromTeaTypeId)
    }

    // The misclick: tapped Green by mistake, meant Black. Green's 75 must not survive.
    @Test
    fun correctingAMistappedTypeReplacesTheStrandedDefault() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = "75.0",
            currentFromTeaTypeId = "green",
            newTeaTypeId = "black",
            newTypeDefaultCelsius = 95.0,
        )
        assertEquals("95.0", seed.celsius)
        assertEquals("black", seed.fromTeaTypeId)
    }

    @Test
    fun aTypedTemperatureSurvivesATypeChange() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = "85",
            currentFromTeaTypeId = null,
            newTeaTypeId = "black",
            newTypeDefaultCelsius = 95.0,
        )
        assertEquals("85", seed.celsius)
        assertNull(seed.fromTeaTypeId)
    }

    @Test
    fun aTypeWithNoDefaultClearsAStrandedSeed() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = "75.0",
            currentFromTeaTypeId = "green",
            newTeaTypeId = "custom",
            newTypeDefaultCelsius = null,
        )
        assertEquals("", seed.celsius)
        assertNull(seed.fromTeaTypeId)
    }

    @Test
    fun aTypeWithNoDefaultLeavesATypedValueAlone() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = "85",
            currentFromTeaTypeId = null,
            newTeaTypeId = "custom",
            newTypeDefaultCelsius = null,
        )
        assertEquals("85", seed.celsius)
        assertNull(seed.fromTeaTypeId)
    }

    // Clearing the field hands ownership back: there is nothing left to protect, so the next type
    // seeds it again.
    @Test
    fun clearingTheFieldLetsTheNextTypeSeedItAgain() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = "",
            currentFromTeaTypeId = null,
            newTeaTypeId = "black",
            newTypeDefaultCelsius = 95.0,
        )
        assertEquals("95.0", seed.celsius)
        assertEquals("black", seed.fromTeaTypeId)
    }

    @Test
    fun reselectingTheSameTypeIsStable() {
        val first = seedTemperatureForTeaType("", null, "green", 75.0)
        val second = seedTemperatureForTeaType(
            currentCelsius = first.celsius,
            currentFromTeaTypeId = first.fromTeaTypeId,
            newTeaTypeId = "green",
            newTypeDefaultCelsius = 75.0,
        )
        assertEquals(first, second)
    }

    @Test
    fun theErrorMessageQuotesTheUnitTheUserIsTypingIn() {
        assertEquals(
            "Temperature must be 0-100°C",
            validateTemperature("500", TemperatureUnit.CELSIUS),
        )
        assertEquals(
            "Temperature must be 32-212°F",
            validateTemperature("500", TemperatureUnit.FAHRENHEIT),
        )
    }
}
