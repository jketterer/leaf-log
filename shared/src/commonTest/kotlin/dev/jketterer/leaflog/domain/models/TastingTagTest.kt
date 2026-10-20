package dev.jketterer.leaflog.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals

class TastingTagTest {

    @Test
    fun `tapping a carried-over tag confirms it`() {
        val tags = listOf(SteepTag("floral", confirmed = false), SteepTag("sweet", confirmed = false))

        assertEquals(
            listOf(SteepTag("floral", confirmed = true), SteepTag("sweet", confirmed = false)),
            tags.toggled("floral"),
        )
    }

    @Test
    fun `tapping a confirmed tag removes it`() {
        val tags = listOf(SteepTag("floral", confirmed = true), SteepTag("sweet", confirmed = false))

        assertEquals(listOf(SteepTag("sweet", confirmed = false)), tags.toggled("floral"))
    }

    @Test
    fun `tapping a tag the steep doesn't have adds it confirmed`() {
        assertEquals(listOf(SteepTag("floral", confirmed = true)), emptyList<SteepTag>().toggled("floral"))
    }

    @Test
    fun `the curated vocabulary is 23 distinct tags`() {
        assertEquals(23, CURATED_TAG_KEYS.toSet().size)
    }
}
