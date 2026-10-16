package dev.jketterer.leaflog.presentation.ui.screens.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

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
    fun `no capacity is valid`() {
        assertNull(validateCapacity(null))
    }

    @Test
    fun `a zero capacity is rejected`() {
        assertNotNull(validateCapacity(0.0))
        assertNull(validateCapacity(0.5))
    }

    // An untouched field keeps the stored value exactly, so a pristine vessel shows no changes.
    @Test
    fun `an untouched stored capacity is not a change`() {
        val vessel = BrewingVessel(
            id = "v",
            name = "Gaiwan",
            iconName = DEFAULT_ICON_NAME,
            capacityMl = 296,
            isSystemDefault = false,
            displayOrder = 0,
            createdAt = Instant.fromEpochMilliseconds(0),
            updatedAt = Instant.fromEpochMilliseconds(0),
        )
        val state = EditVesselState(
            existingVessel = vessel,
            name = vessel.name,
            selectedIconName = DEFAULT_ICON_NAME,
            capacityMl = 296.0,
        )
        assertFalse(state.hasChanges)
        // 10 fl oz typed over it stores as the same whole milliliters.
        assertFalse(state.copy(capacityMl = 295.735).hasChanges)
    }
}
