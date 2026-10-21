package dev.jketterer.leaflog.presentation.ui.screens.steepcomplete

import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SteepCompleteActionsTest {
    private val single = BrewingPlan(base = 180.seconds)
    private val gongfu = BrewingPlan(base = 20.seconds, increment = 5.seconds)

    private fun withMethod(planSessionId: String? = null, methodId: String? = null) = previewSession().let {
        it.copy(session = it.session.copy(usedConfigurationId = methodId, planSessionId = planSessionId))
    }

    private fun vessel(capacityMl: Int?) = BrewingVessel(
        id = "v",
        name = "Gaiwan",
        iconName = null,
        capacityMl = capacityMl,
        isSystemDefault = false,
        displayOrder = 0,
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
    )

    @Test
    fun `finish leads a single steep method`() {
        assertTrue(finishLeads(withMethod(methodId = "m"), single))
    }

    @Test
    fun `finish leads a single steep brew again`() {
        assertTrue(finishLeads(withMethod(planSessionId = "s"), single))
    }

    @Test
    fun `next leads a session with no plan even when it looks like a single steep`() {
        assertFalse(finishLeads(withMethod(), single))
    }

    @Test
    fun `next leads a gongfu method`() {
        assertFalse(finishLeads(withMethod(methodId = "m"), gongfu))
    }

    @Test
    fun `the next steep follows the plan from the current one`() {
        val second = withMethod(methodId = "m").let { it.copy(steeps = previewSession(ratings = listOf(null, null)).steeps) }
        assertEquals(30.seconds, nextSteepTime(second, gongfu))
    }

    @Test
    fun `a single steep method offers no next steep`() {
        assertNull(nextSteepTime(withMethod(methodId = "m"), single))
    }

    @Test
    fun `a session with no plan offers the same steep again`() {
        assertEquals(180.seconds, nextSteepTime(withMethod(), single))
    }

    @Test
    fun `a vessel's capacity replaces the water`() {
        assertEquals(120.0, waterForVessel(200.0, vessel(capacityMl = 120)))
    }

    @Test
    fun `a vessel with no capacity leaves the water`() {
        assertEquals(200.0, waterForVessel(200.0, vessel(capacityMl = null)))
    }
}
