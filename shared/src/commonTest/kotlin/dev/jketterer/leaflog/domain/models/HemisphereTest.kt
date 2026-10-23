package dev.jketterer.leaflog.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals

class HemisphereTest {

    @Test
    fun `a southern region's seasons are southern`() {
        assertEquals(
            listOf(Hemisphere.SOUTHERN, Hemisphere.SOUTHERN, Hemisphere.SOUTHERN),
            listOf("AU", "nz", "BR").map(Hemisphere::forRegion),
        )
    }

    @Test
    fun `a northern or unknown or missing region is northern`() {
        assertEquals(
            listOf(Hemisphere.NORTHERN, Hemisphere.NORTHERN, Hemisphere.NORTHERN),
            listOf("US", "ZZ", "").map(Hemisphere::forRegion),
        )
    }
}
