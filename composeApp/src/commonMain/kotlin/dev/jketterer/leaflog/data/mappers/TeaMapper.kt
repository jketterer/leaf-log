package dev.jketterer.leaflog.data.mappers

import dev.jketterer.leaflog.data.local.database.entities.TeaEntity
import dev.jketterer.leaflog.domain.models.Tea

fun TeaEntity.toTea(): Tea {
    return Tea(
        id = id,
        name = name,
        teaTypeId = teaTypeId,
        origin = origin,
        producer = producer,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        stockAmount = stockAmount,
        defaultBrewingTime = defaultBrewingTime,
        defaultTemperatureCelsius = defaultTemperatureCelsius,
        defaultQuantity = defaultQuantity,
        description = description,
        photos = photos,
        isFavorite = isFavorite,
        totalSessions = totalSessions,
        averageRating = averageRating,
        lastBrewedAt = lastBrewedAt,
        userId = userId,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

fun Tea.toEntity(): TeaEntity {
    return TeaEntity(
        id = id,
        name = name,
        teaTypeId = teaTypeId,
        origin = origin,
        producer = producer,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        stockAmount = stockAmount,
        defaultBrewingTime = defaultBrewingTime,
        defaultTemperatureCelsius = defaultTemperatureCelsius,
        defaultQuantity = defaultQuantity,
        description = description,
        photos = photos,
        isFavorite = isFavorite,
        totalSessions = totalSessions,
        averageRating = averageRating,
        lastBrewedAt = lastBrewedAt,
        userId = userId,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}