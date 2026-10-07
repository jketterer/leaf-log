package dev.jketterer.leaflog.domain.usecases.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class GetSessionReminderDelayUseCaseTest {

    private val getDelay = GetSessionReminderDelayUseCase()

    @Test
    fun `an unknown temperature gets the midpoint`() {
        assertEquals(17.minutes + 30.seconds, getDelay(null))
    }

    @Test
    fun `the coolest brew gets the shortest delay`() {
        assertEquals(5.minutes, getDelay(70.0))
    }

    @Test
    fun `the hottest brew gets the longest delay`() {
        assertEquals(30.minutes, getDelay(95.0))
    }

    @Test
    fun `temperatures in between scale linearly`() {
        assertEquals(15.minutes, getDelay(80.0))
        assertEquals(17.minutes + 30.seconds, getDelay(82.5))
    }

    @Test
    fun `temperatures below the range clamp to the shortest delay`() {
        assertEquals(5.minutes, getDelay(60.0))
    }

    @Test
    fun `temperatures above the range clamp to the longest delay`() {
        assertEquals(30.minutes, getDelay(100.0))
    }
}
