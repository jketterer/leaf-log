package dev.jketterer.leaflog.domain.usecases.configuration

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Generates a descriptive label for a brewing configuration based on its parameters.
 *
 * When [teaName] is provided the label is personalised ("Jasmine · Gong-fu").
 * Without it a style-only label is returned ("Gong-fu"), which serves as the
 * fallback path inside [SaveBrewingConfigurationUseCase].
 */
class GenerateConfigurationLabelUseCase {
    operator fun invoke(
        teaName: String = "",
        teaQuantityGrams: Float?,
        waterQuantityMl: Double,
        brewingTime: Duration,
    ): String {
        val ratio = teaQuantityGrams?.let { it.toDouble() / waterQuantityMl } ?: 0.0

        val style = when {
            // Gong-fu: short time + high leaf ratio
            brewingTime <= 45.seconds && ratio > 0.03 -> "Gong-fu"

            // Western: long steep + large volume
            brewingTime >= 3.minutes && waterQuantityMl >= 250.0 -> "Western"

            // Tea bag: no quantity specified
            teaQuantityGrams == null -> "Bag Method"

            // Grandpa style: long steep + moderate volume
            brewingTime >= 2.minutes && waterQuantityMl in 150.0..350.0 -> "Grandpa"

            // Short steeps
            brewingTime <= 1.minutes -> "Quick Steep"

            // Medium steeps
            brewingTime <= 2.minutes -> "Short Steep"

            // Everything else: label by time
            else -> "Standard"
        }

        return if (teaName.isNotBlank()) "$teaName · $style" else style
    }
}
