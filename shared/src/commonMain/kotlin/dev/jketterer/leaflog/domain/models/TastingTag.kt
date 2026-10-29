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
 * The tags confirmed in the most completed sessions, at most [limit], ties in [vocabulary] order.
 * A tag counts once per session however many steeps confirmed it, so a ten-steep gongfu session
 * weighs no more than a western one. Null when no session has a confirmed tag.
 */
fun List<SessionWithSteeps>.flavorProfile(vocabulary: List<TastingTag>, limit: Int = 8): FlavorProfile? {
    val tagged = completedTagSets()
    val counts = tagged.flatten().groupingBy { it }.eachCount()
    val tags = vocabulary.mostCounted(counts, limit).map { it to counts.getValue(it.id) }
    return if (tags.isEmpty()) null else FlavorProfile(tags, sessions = tagged.size)
}

/** Where one tag turns up in the journal, counted as [flavorProfile] counts. */
data class FlavorUsage(
    /** The completed sessions that confirmed it. */
    val sessions: Int,
    /** The completed sessions with any confirmed tag: what [sessions] is out of. */
    val tagged: Int,
    /** The ids of the teas whose sessions confirmed it most, at most five, with how many. */
    val teas: List<Pair<String, Int>>,
    /** The latest five sessions that confirmed it. */
    val recent: List<SessionWithSteeps>,
)

/** [tagId]'s usage across these sessions, newest first as the journal reads them. */
fun List<SessionWithSteeps>.usageOf(tagId: String): FlavorUsage {
    val completed = filter { it.session.status == SessionStatus.COMPLETED }
    val with = completed.filter { tagId in it.confirmedTagIds() }
    return FlavorUsage(
        sessions = with.size,
        tagged = completedTagSets().size,
        teas = with.groupingBy { it.session.teaId }.eachCount().entries
            .sortedByDescending { it.value }.take(5).map { it.key to it.value },
        recent = with.take(5),
    )
}

/** The tags any of this session's steeps confirmed, each once. */
fun SessionWithSteeps.confirmedTagIds(): Set<String> =
    steeps.flatMap { steep -> steep.tags.filter { it.confirmed }.map { it.tagId } }.toSet()

private fun List<SessionWithSteeps>.completedTagSets() =
    filter { it.session.status == SessionStatus.COMPLETED }.map { it.confirmedTagIds() }.filter { it.isNotEmpty() }

/** This profile's tags missing from [previous]'s; none without a previous profile to compare. */
fun FlavorProfile.newSince(previous: FlavorProfile?): Set<String> {
    if (previous == null) return emptySet()
    val before = previous.tags.map { it.first.id }.toSet()
    return tags.map { it.first.id }.filterNot { it in before }.toSet()
}

private fun List<TastingTag>.mostCounted(counts: Map<String, Int>, limit: Int): List<TastingTag> =
    filter { it.id in counts }.sortedByDescending { counts.getValue(it.id) }.take(limit)
