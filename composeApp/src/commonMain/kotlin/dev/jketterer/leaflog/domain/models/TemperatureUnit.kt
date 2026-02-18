package dev.jketterer.leaflog.domain.models

import kotlin.math.roundToInt

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT;


    fun toggle(): TemperatureUnit = when (this) {
        CELSIUS -> FAHRENHEIT
        FAHRENHEIT -> CELSIUS
    }

    fun toCelsius(value: Int): Int = when (this) {
        CELSIUS -> value
        FAHRENHEIT -> ((value - 32) * 5.0 / 9.0).roundToInt()
    }

    fun fromCelsius(celsius: Int): Int = when (this) {
        CELSIUS -> celsius
        FAHRENHEIT -> ((celsius * 9.0 / 5.0) + 32).roundToInt()
    }

    val symbol: String
        get() = when (this) {
            CELSIUS -> "°C"
            FAHRENHEIT -> "°F"
        }
}
