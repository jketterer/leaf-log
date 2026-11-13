package dev.jketterer.leaflog.presentation.ui.components.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class QuickAdjustButtonsTest {

    @Test
    fun `a steep under a minute adjusts by seconds`() {
        assertEquals(
            listOf((-10).seconds, (-5).seconds, 5.seconds, 10.seconds),
            adjustSteps(59.seconds),
        )
    }

    @Test
    fun `a steep of a minute or more adjusts by half minutes and minutes`() {
        val westernSteps = listOf((-1).minutes, (-30).seconds, 30.seconds, 1.minutes)
        assertEquals(westernSteps, adjustSteps(1.minutes))
        assertEquals(westernSteps, adjustSteps(4.minutes))
    }
}
