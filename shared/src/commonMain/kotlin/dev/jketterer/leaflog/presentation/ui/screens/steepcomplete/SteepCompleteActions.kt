package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TastingTag
import kotlin.time.Duration

/**
 * Whether Finish leads the review instead of Next: a session that follows a method or a Brew Again
 * source whose plan is a single steep. A session with no plan could be either, so Next leads.
 */
internal fun finishLeads(session: SessionWithSteeps, plan: BrewingPlan): Boolean =
    (session.session.usedConfigurationId != null || session.session.planSessionId != null) &&
        plan.increment == null && plan.overrides.isEmpty()

/** The steep after the current one, as the lock screen offers it: none when Finish leads. */
internal fun nextSteepTime(session: SessionWithSteeps, plan: BrewingPlan): Duration? =
    plan.timeOf(session.currentSteep.number + 1).takeUnless { finishLeads(session, plan) }

/** The water a swapped-in [vessel] starts with: its capacity, or [current] when it has none. */
internal fun waterForVessel(current: Double?, vessel: BrewingVessel): Double? =
    vessel.capacityMl?.toDouble() ?: current

/** How many tags the review offers ready to tap, before More. */
internal const val TAG_SUGGESTIONS = 8

/**
 * The tags the review offers ready to tap: those confirmed most often on this tea's steeps
 * ([teaSessions]) first, then most often on any ([allSessions]), then the vocabulary's own order.
 */
internal fun suggestTags(
    vocabulary: List<TastingTag>,
    teaSessions: List<SessionWithSteeps>,
    allSessions: List<SessionWithSteeps>,
    count: Int = TAG_SUGGESTIONS,
): List<TastingTag> {
    val forTea = confirmedTagCounts(teaSessions)
    val overall = confirmedTagCounts(allSessions)
    // A stable sort, so tags used equally keep the vocabulary's order
    return vocabulary
        .sortedWith(compareByDescending<TastingTag> { forTea[it.id] ?: 0 }.thenByDescending { overall[it.id] ?: 0 })
        .take(count)
}

private fun confirmedTagCounts(sessions: List<SessionWithSteeps>): Map<String, Int> =
    sessions.flatMap { it.steeps }.flatMap { it.tags }.filter { it.confirmed }.groupingBy { it.tagId }.eachCount()
