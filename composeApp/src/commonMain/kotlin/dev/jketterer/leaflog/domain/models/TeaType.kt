package dev.jketterer.leaflog.domain.models

import kotlin.time.Instant

data class TeaType(
    val id: String,
    val name: String,
    val defaultTemperatureCelsius: Int? = null,
    val colorHex: String = "",
    val isSystemDefault: Boolean = false,
    val displayOrder: Int = 0,

    val userId: String? = null,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0),
    val updatedAt: Instant = Instant.fromEpochMilliseconds(0),
    val deletedAt: Instant? = null,
)