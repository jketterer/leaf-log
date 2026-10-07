package dev.jketterer.leaflog.domain.usecases.session

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class GetSessionReminderDelayUseCase {
    /**
     * Delay for a brew at [temperatureCelsius], or the midpoint when the temperature is
     * unknown (quick timers carry no session details).
     */
    operator fun invoke(temperatureCelsius: Double?): Duration {
        if (temperatureCelsius == null) return (MIN_DELAY + MAX_DELAY) / 2

        val range = HOTTEST_BREW_CELSIUS - COOLEST_BREW_CELSIUS
        val position = ((temperatureCelsius - COOLEST_BREW_CELSIUS) / range).coerceIn(0.0, 1.0)
        return MIN_DELAY + (MAX_DELAY - MIN_DELAY) * position
    }

    companion object {
        private val MIN_DELAY = 5.minutes
        private val MAX_DELAY = 30.minutes

        private const val COOLEST_BREW_CELSIUS = 70.0
        private const val HOTTEST_BREW_CELSIUS = 95.0
    }
}