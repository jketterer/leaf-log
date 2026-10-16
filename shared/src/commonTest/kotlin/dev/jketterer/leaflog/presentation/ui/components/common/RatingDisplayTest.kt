package dev.jketterer.leaflog.presentation.ui.components.common

import kotlin.test.Test
import kotlin.test.assertEquals

class RatingDisplayTest {

    @Test
    fun `stars round to the nearest half`() {
        assertEquals(8, halfStars(4.2f))
        assertEquals(9, halfStars(4.3f))
        assertEquals(9, halfStars(4.7f))
        assertEquals(10, halfStars(4.8f))
    }

    @Test
    fun `a quarter rounds up to the next half`() {
        assertEquals(9, halfStars(4.25f))
        assertEquals(10, halfStars(4.75f))
    }

    @Test
    fun `whole and half ratings fill exactly`() {
        assertEquals(2, halfStars(1f))
        assertEquals(7, halfStars(3.5f))
        assertEquals(10, halfStars(5f))
    }

    @Test
    fun `the value keeps one decimal and drops a trailing zero`() {
        assertEquals("4.3", formatRating(4.3f))
        assertEquals("4", formatRating(4f))
        assertEquals("4.5", formatRating(4.5f))
    }

    @Test
    fun `the value rounds rather than truncates`() {
        assertEquals("4.5", formatRating(4.46f))
        assertEquals("5", formatRating(4.96f))
        assertEquals("3.7", formatRating(11f / 3))
    }
}
