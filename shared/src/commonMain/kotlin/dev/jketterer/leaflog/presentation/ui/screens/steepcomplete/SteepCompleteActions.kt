package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionWithSteeps

/**
 * Whether Finish leads the review instead of Next: a session that follows a method or a Brew Again
 * source whose plan is a single steep. A session with no plan could be either, so Next leads.
 */
internal fun finishLeads(session: SessionWithSteeps, plan: BrewingPlan): Boolean =
    (session.session.usedConfigurationId != null || session.session.planSessionId != null) &&
        plan.increment == null && plan.overrides.isEmpty()

/** The water a swapped-in [vessel] starts with: its capacity, or [current] when it has none. */
internal fun waterForVessel(current: Double?, vessel: BrewingVessel): Double? =
    vessel.capacityMl?.toDouble() ?: current
