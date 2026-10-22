package dev.jketterer.leaflog.presentation.ui.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchTest {

    @Test
    fun `every word must match some field but not the same one`() {
        val fields = listOf("Da Hong Pao", null, "Roasty and long")

        assertTrue(matchesQuery("hong long", fields))
        assertFalse(matchesQuery("hong sweet", fields))
    }

    @Test
    fun `case and accents are ignored on both sides`() {
        assertTrue(matchesQuery("tie guan yin", listOf("Tiě Guān Yīn")))
        assertTrue(matchesQuery("CRÈME", listOf("creme brulee notes")))
    }

    @Test
    fun `a blank query matches anything`() {
        assertTrue(matchesQuery("  ", emptyList()))
    }
}
