package dev.jketterer.leaflog.data.mappers

import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.domain.models.TeaSession

fun TeaSessionEntity.toTeaSession(): TeaSession {
    return TeaSession(
        id = id,
        teaId = teaId,
        parentSessionId = parentSessionId,
        steepNumber = steepNumber,
        status = status,
        teaQuantityGrams = teaQuantityGrams,
        vesselId = vesselId,
        waterType = waterType,
        location = location,
        rating = rating,
        averageRating = averageRating,
        usedConfigurationId = usedConfigurationId,
        timestamp = timestamp,
        brewingTime = brewingTime,
        temperatureCelsius = temperatureCelsius,
        waterQuantityMl = waterQuantityMl,
        notes = notes,
        photos = photos,
        userId = userId,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        timerStatus = timerStatus,
        timerStartedAt = timerStartedAt,
        timerPausedAt = timerPausedAt,
        timerRemainingMs = timerRemainingMs,
        timerTotalMs = timerTotalMs,
    )
}

fun TeaSession.toEntity(): TeaSessionEntity {
    return TeaSessionEntity(
        id = id,
        teaId = teaId,
        parentSessionId = parentSessionId,
        steepNumber = steepNumber,
        status = status,
        teaQuantityGrams = teaQuantityGrams,
        vesselId = vesselId,
        waterType = waterType,
        location = location,
        rating = rating,
        averageRating = averageRating,
        usedConfigurationId = usedConfigurationId,
        timestamp = timestamp,
        brewingTime = brewingTime,
        temperatureCelsius = temperatureCelsius,
        waterQuantityMl = waterQuantityMl,
        notes = notes,
        photos = photos,
        userId = userId,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        timerStatus = timerStatus,
        timerStartedAt = timerStartedAt,
        timerPausedAt = timerPausedAt,
        timerRemainingMs = timerRemainingMs,
        timerTotalMs = timerTotalMs,
    )
}