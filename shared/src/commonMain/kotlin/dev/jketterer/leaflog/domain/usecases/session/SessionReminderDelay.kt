package dev.jketterer.leaflog.domain.usecases.session

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * How long after a steep finishes to remind someone that the session is still open.
 *
 * The delay tracks brewing temperature as a rough stand-in for how long the cup takes to
 * become drinkable: a near-boiling black tea is still too hot when a cooler green tea is
 * ready. The aim is to arrive while the tea is fresh enough to rate honestly, so the window
 * stays narrow at both ends.
 */
object SessionReminderDelay {

    private val MIN_DELAY = 5.minutes
    private val MAX_DELAY = 15.minutes

    private const val COOLEST_BREW_CELSIUS = 70.0
    private const val HOTTEST_BREW_CELSIUS = 95.0

    /**
     * Delay for a brew at [temperatureCelsius], or the midpoint when the temperature is
     * unknown (quick timers carry no session details).
     */
    fun forBrewingTemperature(temperatureCelsius: Double?): Duration {
        if (temperatureCelsius == null) return (MIN_DELAY + MAX_DELAY) / 2

        val range = HOTTEST_BREW_CELSIUS - COOLEST_BREW_CELSIUS
        val position = ((temperatureCelsius - COOLEST_BREW_CELSIUS) / range).coerceIn(0.0, 1.0)
        return MIN_DELAY + (MAX_DELAY - MIN_DELAY) * position
    }
}
