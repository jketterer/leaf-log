package dev.jketterer.leaflog.data.mappers

import dev.jketterer.leaflog.data.local.database.entities.TeaTypeEntity
import dev.jketterer.leaflog.domain.models.TeaType

fun TeaTypeEntity.toTeaType(): TeaType {
    return TeaType(
        id = id,
        name = name,
        defaultTemperatureCelsius = defaultTemperatureCelsius,
        colorHex = colorHex,
        isSystemDefault = isSystemDefault,
        displayOrder = displayOrder,
        userId = userId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

fun TeaType.toEntity(): TeaTypeEntity {
    return TeaTypeEntity(
        id = id,
        name = name,
        defaultTemperatureCelsius = defaultTemperatureCelsius,
        colorHex = colorHex,
        isSystemDefault = isSystemDefault,
        displayOrder = displayOrder,
        userId = userId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}