package dev.jketterer.leaflog.domain.models

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * How long each steep brews. Steep 1 is [base]; steep N is its override, otherwise steep N-1 plus
 * [increment], so the plan carries on from wherever an override left it. A null increment is a
 * single western steep, and asking for a further one gets the same time again.
 */
data class BrewingPlan(
    val base: Duration,
    val increment: Duration? = null,
    val overrides: Map<Int, Duration> = emptyMap(),
) {
    fun timeOf(steep: Int): Duration {
        var time = base
        for (n in 2..steep) {
            time = overrides[n] ?: (time + (increment ?: Duration.ZERO)).coerceAtLeast(MIN_STEEP)
        }
        return time
    }

    /** How many steeps a preview lists: a few, or as far as the last override so none hides. */
    val previewLength: Int get() = maxOf(PREVIEW_STEEPS, overrides.keys.maxOrNull() ?: 0)

    companion object {
        private const val PREVIEW_STEEPS = 6

        /**
         * The plan that replays [steeps] exactly as they brewed, so a late tap doesn't become an override
         * replayed forever: each time is rounded to 1s under 15s and to 5s from there. The increment is
         * the most common step between neighbors, and any steep that doesn't follow it is an override.
         */
        fun learnedFrom(steeps: List<Steep>): BrewingPlan {
            val times = steeps.map { it.brewedTime.roundedForPlan() }
            val base = times.first()
            val increment = times.zipWithNext { a, b -> b - a }
                .groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key
                ?.takeIf { it != Duration.ZERO }

            val overrides = mutableMapOf<Int, Duration>()
            var expected = base
            times.drop(1).forEachIndexed { i, time ->
                if (time != (expected + (increment ?: Duration.ZERO))) overrides[i + 2] = time
                expected = time
            }
            return BrewingPlan(base, increment, overrides)
        }
    }
}

private val MIN_STEEP = 1.seconds
private val ROUND_TO_5S_FROM = 15.seconds

internal fun Duration.roundedForPlan(): Duration {
    val step = if (this < ROUND_TO_5S_FROM) 1000L else 5000L
    val rounded = (inWholeMilliseconds + step / 2) / step * step
    return rounded.milliseconds.coerceAtLeast(MIN_STEEP)
}
