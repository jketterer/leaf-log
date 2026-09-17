package dev.jketterer.leaflog.presentation.ui.components.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DurationFieldsTest {

    @Test
    fun bothFieldsEmptyIsNoDuration() {
        assertNull(parseDurationFields("", ""))
    }

    @Test
    fun parsesMinutesAndSeconds() {
        assertEquals(2.minutes + 30.seconds, parseDurationFields("2", "30"))
    }

    @Test
    fun emptyFieldCountsAsZero() {
        assertEquals(45.seconds, parseDurationFields("", "45"))
        assertEquals(2.minutes, parseDurationFields("2", ""))
    }

    @Test
    fun zerosAreADurationNotNull() {
        assertEquals(Duration.ZERO, parseDurationFields("0", "0"))
    }

    @Test
    fun nonNumericTextIsNoDuration() {
        assertNull(parseDurationFields("abc", "30"))
        assertNull(parseDurationFields("2", "abc"))
    }

    @Test
    fun secondsAreClampedToOneMinute() {
        assertEquals(2.minutes + 59.seconds, parseDurationFields("2", "99"))
    }

    @Test
    fun splitsDurationIntoFieldText() {
        assertEquals("2" to "30", durationToFields(2.minutes + 30.seconds))
        assertEquals("0" to "45", durationToFields(45.seconds))
        assertEquals("1" to "30", durationToFields(90.seconds))
    }

    @Test
    fun fieldTextRoundTripsBackToTheSameDuration() {
        listOf(45.seconds, 2.minutes, 2.minutes + 30.seconds, Duration.ZERO).forEach { duration ->
            val (minutesText, secondsText) = durationToFields(duration)
            assertEquals(duration, parseDurationFields(minutesText, secondsText))
        }
    }
}
