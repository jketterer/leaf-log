package dev.jketterer.leaflog.data.mappers

import dev.jketterer.leaflog.data.local.database.entities.BrewingVesselEntity
import dev.jketterer.leaflog.domain.models.BrewingVessel

fun BrewingVesselEntity.toBrewingVessel(): BrewingVessel {
    return BrewingVessel(
        id = id,
        name = name,
        iconName = iconName,
        imagePath = imagePath,
        capacityMl = capacityMl,
        isSystemDefault = isSystemDefault,
        displayOrder = displayOrder,
        userId = userId,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

fun BrewingVessel.toEntity(): BrewingVesselEntity {
    return BrewingVesselEntity(
        id = id,
        name = name,
        iconName = iconName,
        imagePath = imagePath,
        capacityMl = capacityMl,
        isSystemDefault = isSystemDefault,
        displayOrder = displayOrder,
        userId = userId,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}