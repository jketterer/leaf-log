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

/**
 * The session's tags confirmed on the most steeps, at most three, ties in [vocabulary] order. A
 * carry-over nobody tapped counts for nothing, as an untouched rating does.
 */
fun SessionWithSteeps.dominantTags(vocabulary: List<TastingTag>): List<TastingTag> {
    val counts = steeps.flatMap { steep -> steep.tags.filter { it.confirmed }.map { it.tagId } }
        .groupingBy { it }.eachCount()
    return vocabulary.mostCounted(counts, limit = 3)
}

/** A tea's most confirmed tags, each with the number of [sessions] it was confirmed in. */
data class FlavorProfile(
    val tags: List<Pair<TastingTag, Int>>,
    /** The completed sessions with any confirmed tag: what each tag's count is out of. */
    val sessions: Int,
)

/**
 * The tags confirmed in the most completed sessions, at most eight, ties in [vocabulary] order.
 * A tag counts once per session however many steeps confirmed it, so a ten-steep gongfu session
 * weighs no more than a western one. Null when no session has a confirmed tag.
 */
fun List<SessionWithSteeps>.flavorProfile(vocabulary: List<TastingTag>): FlavorProfile? {
    val tagged = filter { it.session.status == SessionStatus.COMPLETED }
        .map { session -> session.steeps.flatMap { steep -> steep.tags.filter { it.confirmed }.map { it.tagId } }.toSet() }
        .filter { it.isNotEmpty() }
    val counts = tagged.flatten().groupingBy { it }.eachCount()
    val tags = vocabulary.mostCounted(counts, limit = 8).map { it to counts.getValue(it.id) }
    return if (tags.isEmpty()) null else FlavorProfile(tags, sessions = tagged.size)
}

private fun List<TastingTag>.mostCounted(counts: Map<String, Int>, limit: Int): List<TastingTag> =
    filter { it.id in counts }.sortedByDescending { counts.getValue(it.id) }.take(limit)
