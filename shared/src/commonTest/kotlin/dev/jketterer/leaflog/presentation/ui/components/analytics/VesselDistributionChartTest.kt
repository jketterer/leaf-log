package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.ui.graphics.Color
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.VesselDistribution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class VesselDistributionChartTest {

    private val palette = listOf(Color.Red, Color.Green)

    private fun distribution(vararg counts: Int) = counts.mapIndexed { index, count ->
        VesselDistribution(
            vessel = BrewingVessel(
                id = "v$index",
                name = "Vessel $index",
                iconName = null,
                isSystemDefault = false,
                displayOrder = index,
                createdAt = Instant.fromEpochSeconds(0),
                updatedAt = Instant.fromEpochSeconds(0),
            ),
            sessionCount = count,
            percentage = count * 100f / counts.sum(),
        )
    }

    private fun slices(vararg counts: Int) =
        vesselSlices(distribution(*counts), palette, otherLabel = "Other", otherColor = Color.Gray)

    @Test
    fun `vessels that fit the palette take its colors in order with no other slice`() {
        val slices = slices(3, 1)

        assertEquals(listOf("v0", "v1"), slices.map { it.id })
        assertEquals(palette, slices.map { it.color })
    }

    @Test
    fun `vessels past the palette fold into one untappable other slice`() {
        val other = slices(4, 3, 2, 1).last()

        assertNull(other.id)
        assertEquals("Other", other.label)
        assertEquals(Color.Gray, other.color)
        assertEquals(3, other.count)
        assertEquals(30f, other.percentage, absoluteTolerance = 0.01f)
    }
}
