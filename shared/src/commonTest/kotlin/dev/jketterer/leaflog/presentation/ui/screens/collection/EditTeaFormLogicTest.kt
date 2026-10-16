package dev.jketterer.leaflog.presentation.ui.screens.collection

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
    fun `no temperature is accepted`() {
        assertNull(validateTemperature(null))
    }

    @Test
    fun `freezing to boiling is accepted`() {
        assertNull(validateTemperature(0.0))
        assertNull(validateTemperature(80.0))
        assertNull(validateTemperature(100.0))
    }

    @Test
    fun `below freezing or above boiling is rejected`() {
        assertNotNull(validateTemperature(-0.5))
        assertNotNull(validateTemperature(100.5))
    }

    // --- temperature seeding -------------------------------------------------------------------

    @Test
    fun anEmptyFieldTakesTheTypeDefault() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = null,
            currentFromTeaTypeId = null,
            newTeaTypeId = "green",
            newTypeDefaultCelsius = 75.0,
        )
        assertEquals(75.0, seed.celsius)
        assertEquals("green", seed.fromTeaTypeId)
    }

    // The misclick: tapped Green by mistake, meant Black. Green's 75 must not survive.
    @Test
    fun correctingAMistappedTypeReplacesTheStrandedDefault() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = 75.0,
            currentFromTeaTypeId = "green",
            newTeaTypeId = "black",
            newTypeDefaultCelsius = 95.0,
        )
        assertEquals(95.0, seed.celsius)
        assertEquals("black", seed.fromTeaTypeId)
    }

    @Test
    fun aTypedTemperatureSurvivesATypeChange() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = 85.0,
            currentFromTeaTypeId = null,
            newTeaTypeId = "black",
            newTypeDefaultCelsius = 95.0,
        )
        assertEquals(85.0, seed.celsius)
        assertNull(seed.fromTeaTypeId)
    }

    @Test
    fun aTypeWithNoDefaultClearsAStrandedSeed() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = 75.0,
            currentFromTeaTypeId = "green",
            newTeaTypeId = "custom",
            newTypeDefaultCelsius = null,
        )
        assertNull(seed.celsius)
        assertNull(seed.fromTeaTypeId)
    }

    @Test
    fun aTypeWithNoDefaultLeavesATypedValueAlone() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = 85.0,
            currentFromTeaTypeId = null,
            newTeaTypeId = "custom",
            newTypeDefaultCelsius = null,
        )
        assertEquals(85.0, seed.celsius)
        assertNull(seed.fromTeaTypeId)
    }

    // Clearing the field hands ownership back: there is nothing left to protect, so the next type
    // seeds it again.
    @Test
    fun clearingTheFieldLetsTheNextTypeSeedItAgain() {
        val seed = seedTemperatureForTeaType(
            currentCelsius = null,
            currentFromTeaTypeId = null,
            newTeaTypeId = "black",
            newTypeDefaultCelsius = 95.0,
        )
        assertEquals(95.0, seed.celsius)
        assertEquals("black", seed.fromTeaTypeId)
    }

    @Test
    fun reselectingTheSameTypeIsStable() {
        val first = seedTemperatureForTeaType(null, null, "green", 75.0)
        val second = seedTemperatureForTeaType(
            currentCelsius = first.celsius,
            currentFromTeaTypeId = first.fromTeaTypeId,
            newTeaTypeId = "green",
            newTypeDefaultCelsius = 75.0,
        )
        assertEquals(first, second)
    }
}
