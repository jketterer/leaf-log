package dev.jketterer.leaflog.domain.models

import kotlin.math.roundToInt

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT;


    fun toggle(): TemperatureUnit = when (this) {
        CELSIUS -> FAHRENHEIT
        FAHRENHEIT -> CELSIUS
    }

    /** Converts a display-unit value to Celsius without rounding (preserves precision). */
    fun toCelsius(value: Int): Double = when (this) {
        CELSIUS -> value.toDouble()
        FAHRENHEIT -> (value - 32) * 5.0 / 9.0
    }

    /** Converts a Celsius value to the display unit, rounded to Int for display. */
    fun fromCelsius(celsius: Double): Int = when (this) {
        CELSIUS -> celsius.roundToInt()
        FAHRENHEIT -> (celsius * 9.0 / 5.0 + 32).roundToInt()
    }

    val symbol: String
        get() = when (this) {
            CELSIUS -> "°C"
            FAHRENHEIT -> "°F"
        }
}
