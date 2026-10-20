package dev.jketterer.leaflog.domain.models

/** A word for how a steep tasted: one of [CURATED_TAG_KEYS], or one the user made. */
data class TastingTag(
    val id: String,
    /** Names a curated tag's string; null on a custom tag. */
    val key: String?,
    /** A custom tag's name; null on a curated tag. */
    val name: String?,
    val isCustom: Boolean,
    val displayOrder: Int,
)

/** The curated vocabulary in display order, aromas first and finishes last. Each is its tag's id. */
val CURATED_TAG_KEYS = listOf(
    "floral", "fruity", "honey", "roasted", "smoky", "grassy", "woody", "earthy", "spicy", "sweet",
    "bitter", "astringent", "umami", "mineral", "malty", "nutty", "light", "smooth", "full",
    "lingering", "short", "drying", "returning_sweetness",
)

/** A tag on a steep. One carried over from the steep before stays unconfirmed until it's tapped. */
data class SteepTag(
    val tagId: String,
    val confirmed: Boolean,
)

/** Tapping [tagId]: a carry-over is confirmed, a confirmed tag removed, any other added confirmed. */
fun List<SteepTag>.toggled(tagId: String): List<SteepTag> {
    val tag = find { it.tagId == tagId }
    return when {
        tag == null -> this + SteepTag(tagId, confirmed = true)
        tag.confirmed -> this - tag
        else -> map { if (it.tagId == tagId) it.copy(confirmed = true) else it }
    }
}

/** What the next steep starts with: every tag of this one, unconfirmed. */
fun List<SteepTag>.carriedForward(): List<SteepTag> = map { it.copy(confirmed = false) }
