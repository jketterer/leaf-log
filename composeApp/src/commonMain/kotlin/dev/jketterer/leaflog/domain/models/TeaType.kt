package dev.jketterer.leaflog.domain.models

import kotlin.time.Duration
import kotlin.time.Instant

data class TeaType(
    val id: String,
    val name: String,
    val defaultTemperatureCelsius: Int?,
    val defaultBrewingTime: Duration?,
    val colorHex: String,
    val isSystemDefault: Boolean,
    val displayOrder: Int,

    val userId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)