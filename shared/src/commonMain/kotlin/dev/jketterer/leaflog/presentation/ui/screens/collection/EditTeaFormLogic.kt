package dev.jketterer.leaflog.presentation.ui.screens.collection

import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.error_quantity_positive
import leaf_log.shared.generated.resources.error_temperature_range
import org.jetbrains.compose.resources.StringResource

/** Error message for a tea name, or null when it is acceptable. */
internal fun validateTeaName(name: String): String? =
    if (name.isBlank()) "Name is required" else null

/**
 * Error for a default brewing temperature, or null when it is acceptable. The temperature is
 * optional, so null is valid, not missing.
 */
internal fun validateTemperature(celsius: Double?): StringResource? =
    if (celsius != null && celsius !in 0.0..100.0) Res.string.error_temperature_range else null

/** Error for a required tea or water quantity: [missing] when empty, or one that isn't positive. */
internal fun validateQuantity(value: Double?, missing: StringResource): StringResource? = when {
    value == null -> missing
    value <= 0 -> Res.string.error_quantity_positive
    else -> null
}

/**
 * The temperature field after a tea type is picked, and which type it now came from.
 *
 * [fromTeaTypeId] is the provenance marker: non-null means the number on screen was seeded from
 * that tea type and may be replaced freely, null means the user owns it.
 */
internal data class TemperatureSeed(
    val celsius: Double?,
    val fromTeaTypeId: String?,
)

/**
 * Decides what the temperature field should hold once [newTeaTypeId] is selected.
 */
internal fun seedTemperatureForTeaType(
    currentCelsius: Double?,
    currentFromTeaTypeId: String?,
    newTeaTypeId: String,
    newTypeDefaultCelsius: Double?,
): TemperatureSeed {
    val userOwnsIt = currentCelsius != null && currentFromTeaTypeId == null
    if (userOwnsIt) {
        return TemperatureSeed(currentCelsius, null)
    }
    // A type with no default of its own clears the stale seed rather than leaving another type's
    // number sitting under the wrong label.
    return TemperatureSeed(
        celsius = newTypeDefaultCelsius,
        fromTeaTypeId = newTypeDefaultCelsius?.let { newTeaTypeId },
    )
}
