package dev.jketterer.leaflog.data.mappers

import dev.jketterer.leaflog.data.local.database.entities.BrewingConfigurationEntity
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Instant

fun BrewingConfigurationEntity.toBrewingConfiguration(): BrewingConfiguration {
    return BrewingConfiguration(
        id = id,
        teaId = teaId,
        vesselId = vesselId,
        teaQuantityGrams = teaQuantityGrams,
        waterQuantityMl = waterQuantityMl,
        temperatureCelsius = temperatureCelsius,
        brewingTime = brewingTime,
        waterType = WaterType.valueOf(waterType),
        sourceSessionId = sourceSessionId,
        rating = rating,
        timesUsed = timesUsed,
        lastUsedAt = lastUsedAt?.let { Instant.fromEpochMilliseconds(it) },
        label = label,
        isActive = isActive,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
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
        waterType = waterType.name,
        sourceSessionId = sourceSessionId,
        rating = rating,
        timesUsed = timesUsed,
        lastUsedAt = lastUsedAt?.toEpochMilliseconds(),
        label = label,
        isActive = isActive,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
    )
}
