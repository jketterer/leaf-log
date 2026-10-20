package dev.jketterer.leaflog.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class BrewingPlanTest {

    @Test
    fun `steep times add the increment to the one before`() {
        val plan = BrewingPlan(base = 20.seconds, increment = 5.seconds)

        assertEquals(listOf(20L, 25L, 30L, 35L), (1..4).map { plan.timeOf(it).inWholeSeconds })
    }

    @Test
    fun `an override replaces its steep and the plan carries on from it`() {
        val plan = BrewingPlan(20.seconds, 5.seconds, overrides = mapOf(3 to 45.seconds))

        assertEquals(listOf(20L, 25L, 45L, 50L), (1..4).map { plan.timeOf(it).inWholeSeconds })
    }

    @Test
    fun `a plan without an increment brews every steep the same`() {
        assertEquals(3.minutesInSeconds(), BrewingPlan(3.minutesInSeconds()).timeOf(2))
    }

    @Test
    fun `a learned plan replays every steep it was learned from`() {
        val brewed = listOf(20, 25, 30, 45, 50, 50)
        val plan = BrewingPlan.learnedFrom(steeps(brewed.map { it.seconds }))

        assertEquals(brewed, (1..brewed.size).map { plan.timeOf(it).inWholeSeconds.toInt() })
        assertEquals(20.seconds, plan.base)
        assertEquals(5.seconds, plan.increment)
        assertEquals(mapOf(4 to 45.seconds, 6 to 50.seconds), plan.overrides)
    }

    @Test
    fun `one steep learns no increment`() {
        val plan = BrewingPlan.learnedFrom(steeps(listOf(3.minutesInSeconds())))

        assertNull(plan.increment)
        assertEquals(emptyMap(), plan.overrides)
    }

    @Test
    fun `the same time twice learns no increment either`() {
        assertNull(BrewingPlan.learnedFrom(steeps(listOf(30.seconds, 30.seconds))).increment)
    }

    @Test
    fun `a late tap is rounded to the second under 15s and to 5s from there`() {
        val plan = BrewingPlan.learnedFrom(steeps(listOf(10_400.milliseconds, 22_600.milliseconds, 29_900.milliseconds)))

        assertEquals(10.seconds, plan.base)
        assertEquals(listOf(10, 25, 30), (1..3).map { plan.timeOf(it).inWholeSeconds.toInt() })
    }

    @Test
    fun `the actual time is learned rather than the planned one`() {
        val plan = BrewingPlan.learnedFrom(listOf(steep(1, planned = 20.seconds, actual = 31.seconds)))

        assertEquals(30.seconds, plan.base)
    }

    private fun Int.minutesInSeconds() = (this * 60).seconds
    private fun steeps(times: List<Duration>) = times.mapIndexed { i, t -> steep(i + 1, t) }

    private fun steep(number: Int, planned: Duration, actual: Duration? = null) = Steep(
        id = "s$number",
        number = number,
        timestamp = Instant.fromEpochSeconds(0),
        brewingTime = planned,
        temperatureCelsius = 95.0,
        waterQuantityMl = 100.0,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
        actualTime = actual,
    )
}
