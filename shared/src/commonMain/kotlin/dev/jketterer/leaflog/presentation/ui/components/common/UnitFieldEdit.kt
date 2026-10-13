package dev.jketterer.leaflog.presentation.ui.components.common

/**
 * What a unit field's [text] stands for in stored units, or null when it still reads as [stored]
 * does, or isn't a number. Display units are whole numbers, so converting an untouched field back
 * would nudge the value: 93 °C shows as 199 °F, which reads back as 92.78 °C.
 */
fun editedUnitValue(
    text: String,
    stored: Double,
    toDisplay: (Double) -> Int,
    toStored: (Int) -> Double,
): Double? {
    val shown = text.trim().toIntOrNull() ?: return null
    return if (shown == toDisplay(stored)) null else toStored(shown)
}
