package dev.jketterer.leaflog.domain.models

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT;

    fun toCelsius(value: Int): Int = when (this) {
        CELSIUS -> value
        FAHRENHEIT -> ((value - 32) * 5 / 9)
    }

    fun fromCelsius(celsius: Int): Int = when (this) {
        CELSIUS -> celsius
        FAHRENHEIT -> ((celsius * 9 / 5) + 32)
    }

    val symbol: String
        get() = when (this) {
            CELSIUS -> "°C"
            FAHRENHEIT -> "°F"
        }
}
