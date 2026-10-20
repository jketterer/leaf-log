package dev.jketterer.leaflog.presentation.ui.components.session

import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SteepTagsTest {

    private val labels = listOf(1 to "Sweet", 2 to "Returning sweetness", 3 to "Smoky")

    @Test
    fun `a search matches any part of a tag, ignoring case`() {
        val (matches, _) = searchTags(labels, "SWEET")

        assertEquals(listOf(1, 2), matches.map { it.first })
    }

    @Test
    fun `a word no tag is called can be created`() {
        assertTrue(searchTags(labels, " swee ").second)
    }

    @Test
    fun `a tag's own name or a blank search can't be created`() {
        assertFalse(searchTags(labels, "smoky ").second)
        assertFalse(searchTags(labels, "  ").second)
    }

    @Test
    fun `an empty search shows every tag`() {
        assertEquals(labels, searchTags(labels, "").first)
    }

    @Test
    fun `every curated tag has a name`() {
        assertEquals(CURATED_TAG_KEYS.toSet(), CURATED_TAG_NAMES.keys)
    }
}
