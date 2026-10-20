package dev.jketterer.leaflog.data.mappers

import dev.jketterer.leaflog.data.local.database.entities.BrewingConfigurationEntity
import dev.jketterer.leaflog.domain.models.BrewingConfiguration

fun BrewingConfigurationEntity.toBrewingConfiguration(): BrewingConfiguration {
    return BrewingConfiguration(
        id = id,
        teaId = teaId,
        vesselId = vesselId,
        teaQuantityGrams = teaQuantityGrams,
        waterQuantityMl = waterQuantityMl,
        temperatureCelsius = temperatureCelsius,
        brewingTime = brewingTime,
        steepIncrement = steepIncrement,
        steepOverrides = steepOverrides,
        waterType = waterType,
        rinse = rinse,
        sourceSessionId = sourceSessionId,
        rating = rating,
        timesUsed = timesUsed,
        lastUsedAt = lastUsedAt,
        label = label,
        isPinned = isPinned,
        pinnedSortOrder = pinnedSortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

fun BrewingConfiguration.toEntity(): BrewingConfigurationEntity {
    return BrewingConfigurationEntity(
        id = id,
        teaId = teaId,
        vesselId = vesselId,
        teaQuantityGrams = teaQuantityGrams,
        waterQuantityMl = waterQuantityMl,
        temperatureCelsius = temperatureCelsius,
        brewingTime = brewingTime,
        steepIncrement = steepIncrement,
        steepOverrides = steepOverrides,
        waterType = waterType,
        rinse = rinse,
        sourceSessionId = sourceSessionId,
        rating = rating,
        timesUsed = timesUsed,
        lastUsedAt = lastUsedAt,
        label = label,
        isPinned = isPinned,
        pinnedSortOrder = pinnedSortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
