package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.TemperatureUnit

/** Error message for a tea name, or null when it is acceptable. */
internal fun validateTeaName(name: String): String? =
    if (name.isBlank()) "Name is required" else null

/**
 * Error message for a default brewing temperature, or null when it is acceptable.
 *
 * The temperature is optional (`Tea.defaultTemperatureCelsius` is nullable) so a blank field is
 * valid, not missing. Validation runs in the unit the user is typing in, so the message quotes
 * bounds they recognize.
 */
internal fun validateTemperature(input: String, unit: TemperatureUnit): String? {
    if (input.isBlank()) return null
    val value = input.toIntOrNull() ?: return "Invalid temperature"
    val range = unit.validRange()
    return if (value in range) {
        null
    } else {
        "Temperature must be ${range.first}-${range.last}${unit.symbol}"
    }
}

internal fun TemperatureUnit.validRange(): IntRange = when (this) {
    TemperatureUnit.CELSIUS -> 0..100
    TemperatureUnit.FAHRENHEIT -> 32..212
}

/**
 * The temperature field after a tea type is picked, and which type it now came from.
 *
 * [fromTeaTypeId] is the provenance marker: non-null means the number on screen was seeded from
 * that tea type and may be replaced freely, null means the user owns it.
 */
internal data class TemperatureSeed(
    val celsius: String,
    val fromTeaTypeId: String?,
)

/**
 * Decides what the temperature field should hold once [newTeaTypeId] is selected.
 */
internal fun seedTemperatureForTeaType(
    currentCelsius: String,
    currentFromTeaTypeId: String?,
    newTeaTypeId: String,
    newTypeDefaultCelsius: Double?,
): TemperatureSeed {
    val userOwnsIt = currentCelsius.isNotBlank() && currentFromTeaTypeId == null
    if (userOwnsIt) {
        return TemperatureSeed(currentCelsius, null)
    }
    // A type with no default of its own clears the stale seed rather than leaving another type's
    // number sitting under the wrong label.
    return TemperatureSeed(
        celsius = newTypeDefaultCelsius?.toString() ?: "",
        fromTeaTypeId = newTypeDefaultCelsius?.let { newTeaTypeId },
    )
}
