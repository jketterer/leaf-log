package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration

/**
 * Result of pre-filling brewing parameters from history
 */
data class BrewingParametersPrefill(
    val teaQuantityGrams: Float?,
    val waterQuantityMl: Double?,
    val temperatureCelsius: Double?,
    val brewingTime: Duration?,
    val waterType: WaterType?,
    val source: PrefillSource,
    val rating: Float?,
) {
    /**
     * True if any parameters were pre-filled
     */
    val hasParameters: Boolean
        get() = teaQuantityGrams != null ||
                waterQuantityMl != null ||
                temperatureCelsius != null ||
                brewingTime != null
}
