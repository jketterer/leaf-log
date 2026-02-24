package dev.jketterer.leaflog.domain.models

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * A saved brewing configuration for a tea + vessel combination
 */
data class BrewingConfiguration(
    val id: String,
    val teaId: String,
    val vesselId: String,

    // Brewing parameters
    val teaQuantityGrams: Float?,
    val waterQuantityMl: Double,
    val temperatureCelsius: Double,
    val brewingTime: Duration,
    val waterType: WaterType,

    // Metadata
    val sourceSessionId: String?, // Session this was learned from (null for manually created)
    val rating: Float?, // Rating of that session (null for manually created)
    val timesUsed: Int, // How often user has used this config
    val lastUsedAt: Instant?, // Last time this config was used

    // User customization
    val label: String?, // User-provided or auto-generated name
    val isActive: Boolean, // User can disable configs without deleting

    val createdAt: Instant,
    val updatedAt: Instant,
)
