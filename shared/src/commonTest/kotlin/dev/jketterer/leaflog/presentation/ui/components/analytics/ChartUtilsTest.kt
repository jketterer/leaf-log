package dev.jketterer.leaflog.presentation.ui.components.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChartUtilsTest {

    // A quarter segment from 12 to 3 o'clock, then the rest of the ring
    private val sweeps = listOf(90f, 270f)

    private fun segmentAt(dx: Float, dy: Float) =
        donutSegmentAt(dx, dy, innerRadius = 50f, outerRadius = 80f, sweeps = sweeps)

    @Test
    fun `the first segment starts at 12 o'clock`() {
        assertEquals(0, segmentAt(dx = 1f, dy = -65f))
    }

    @Test
    fun `segments run clockwise`() {
        assertEquals(0, segmentAt(dx = 46f, dy = -46f)) // half past one
        assertEquals(1, segmentAt(dx = 46f, dy = 46f)) // half past four
        assertEquals(1, segmentAt(dx = -46f, dy = -46f)) // half past ten
    }

    @Test
    fun `just before 12 o'clock is the last segment`() {
        assertEquals(1, segmentAt(dx = -1f, dy = -65f))
    }

    @Test
    fun `taps in the hole or outside the ring hit nothing`() {
        assertNull(segmentAt(dx = 0f, dy = -30f))
        assertNull(segmentAt(dx = 0f, dy = -90f))
    }

    @Test
    fun `a tap past the midpoint between points picks the next point`() {
        assertEquals(1, nearestPointIndex(x = 125f, firstX = 30f, stepX = 100f, count = 5))
        assertEquals(2, nearestPointIndex(x = 185f, firstX = 30f, stepX = 100f, count = 5))
    }

    @Test
    fun `taps beyond either end pick the end point`() {
        assertEquals(0, nearestPointIndex(x = 0f, firstX = 30f, stepX = 100f, count = 5))
        assertEquals(4, nearestPointIndex(x = 900f, firstX = 30f, stepX = 100f, count = 5))
    }
}
