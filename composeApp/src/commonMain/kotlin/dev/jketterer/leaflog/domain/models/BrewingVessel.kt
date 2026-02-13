package dev.jketterer.leaflog.domain.models

import kotlin.time.Instant

data class BrewingVessel(
    val id: String,
    val name: String,
    val iconName: String?,
    val imagePath: String? = null,
    val capacityMl: Int? = null,
    val isSystemDefault: Boolean,
    val displayOrder: Int,

    val userId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)