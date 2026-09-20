package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.VolumeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EditVesselStateTest {

    // --- name -----------------------------------------------------------------------------------

    @Test
    fun aBlankNameIsRejected() {
        assertEquals("Name is required", validateVesselName(""))
        assertEquals("Name is required", validateVesselName("   "))
    }

    @Test
    fun aNameOfFiftyCharactersIsStillAccepted() {
        assertNull(validateVesselName("v".repeat(50)))
        assertNotNull(validateVesselName("v".repeat(51)))
    }

    // --- capacity validation --------------------------------------------------------------------

    // Capacity is optional, so an empty field is valid rather than missing.
    @Test
    fun aBlankCapacityIsValid() {
        assertNull(validateCapacity(""))
    }

    @Test
    fun aNonNumericCapacityIsRejected() {
        assertEquals("Must be a valid number", validateCapacity("abc"))
        assertEquals("Must be a valid number", validateCapacity("12.5"))
    }

    @Test
    fun aZeroOrNegativeCapacityIsRejected() {
        assertEquals("Capacity must be greater than 0", validateCapacity("0"))
        assertEquals("Capacity must be greater than 0", validateCapacity("-5"))
    }

    // --- conversion to storage ------------------------------------------------------------------

    @Test
    fun millilitersAreStoredAsTyped() {
        assertEquals(150, capacityToMl("150", VolumeUnit.MILLILITERS))
    }

    @Test
    fun fluidOuncesAreConvertedToMilliliters() {
        assertEquals(296, capacityToMl("10", VolumeUnit.FLUID_OUNCES))
    }

    @Test
    fun anEmptyOrUnusableCapacityHasNoStoredValue() {
        assertNull(capacityToMl("", VolumeUnit.MILLILITERS))
        assertNull(capacityToMl("abc", VolumeUnit.FLUID_OUNCES))
    }

    // The bug this whole shape exists to prevent: a stored vessel, shown in ounces and saved back
    // untouched, has to come out as the same number of milliliters it went in as. Converting on
    // every keystroke instead rounded the value away and faked unsaved changes on a pristine form.
    @Test
    fun aStoredCapacitySurvivesADisplayRoundTripInOunces() {
        val stored = 296
        val shown = capacityFromMl(stored, VolumeUnit.FLUID_OUNCES)

        assertEquals("10", shown)
        assertEquals(stored, capacityToMl(shown, VolumeUnit.FLUID_OUNCES))
    }

    @Test
    fun aVesselWithNoCapacityShowsAnEmptyField() {
        assertEquals("", capacityFromMl(null, VolumeUnit.MILLILITERS))
    }

    // --- unit toggle ----------------------------------------------------------------------------

    @Test
    fun togglingTheUnitConvertsTheNumber() {
        assertEquals(
            "5",
            convertCapacity("150", from = VolumeUnit.MILLILITERS, to = VolumeUnit.FLUID_OUNCES),
        )
        assertEquals(
            "296",
            convertCapacity("10", from = VolumeUnit.FLUID_OUNCES, to = VolumeUnit.MILLILITERS),
        )
    }

    // The preferences flow re-emits on every change, not only on a unit change, so the no-op case
    // has to leave the field alone rather than round it.
    @Test
    fun theSameUnitLeavesTheFieldUntouched() {
        assertEquals(
            "150",
            convertCapacity("150", from = VolumeUnit.MILLILITERS, to = VolumeUnit.MILLILITERS),
        )
    }

    @Test
    fun halfFinishedInputIsLeftExactlyAsTyped() {
        assertEquals(
            "",
            convertCapacity("", from = VolumeUnit.MILLILITERS, to = VolumeUnit.FLUID_OUNCES),
        )
        assertEquals(
            "abc",
            convertCapacity("abc", from = VolumeUnit.MILLILITERS, to = VolumeUnit.FLUID_OUNCES),
        )
    }
}
