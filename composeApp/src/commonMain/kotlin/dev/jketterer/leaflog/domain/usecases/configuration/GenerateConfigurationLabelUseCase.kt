package dev.jketterer.leaflog.domain.usecases.configuration

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Generates a descriptive label for a brewing configuration based on its parameters
 */
class GenerateConfigurationLabelUseCase {
    operator fun invoke(
        teaQuantityGrams: Float?,
        waterQuantityMl: Double,
        brewingTime: Duration,
    ): String {
        val ratio = teaQuantityGrams?.let { it.toDouble() / waterQuantityMl } ?: 0.0

        return when {
            // Gong-fu style: short steep time + high ratio
            brewingTime <= 45.seconds && ratio > 0.03 -> "Gong-fu Style"

            // Western style: long steep time + large volume
            brewingTime >= 3.minutes && waterQuantityMl >= 250.0 -> "Western Style"

            // Tea bag method: no quantity specified
            teaQuantityGrams == null -> "Tea Bag Method"

            // Grandpa style: long steep + moderate volume
            brewingTime >= 2.minutes && waterQuantityMl in 150.0..350.0 -> "Grandpa Style"

            // Quick brew: short time
            brewingTime <= 1.minutes -> "Quick Brew"

            // Default
            else -> "Custom Method"
        }
    }
}
