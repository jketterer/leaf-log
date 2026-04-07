package dev.jketterer.leaflog.domain.usecases.configuration

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Generates a descriptive label for a brewing configuration based on its parameters.
 */
class GenerateConfigurationLabelUseCase {
    operator fun invoke(
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
            teaQuantityGrams == null -> "Tea Bag"

            // Grandpa style: long steep + moderate volume
            brewingTime >= 2.minutes && waterQuantityMl in 150.0..350.0 -> "Grandpa"

            // Short steeps
            brewingTime <= 1.minutes -> "Quick Steep"

            // Everything else: label by time
            else -> "Standard"
        }

        return style
    }
}
