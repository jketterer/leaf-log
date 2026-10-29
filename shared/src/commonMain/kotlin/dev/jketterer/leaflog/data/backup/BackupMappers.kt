package dev.jketterer.leaflog.data.backup

import dev.jketterer.leaflog.data.local.database.entities.BrewingVesselEntity
import dev.jketterer.leaflog.data.local.database.entities.SteepTagEntity
import dev.jketterer.leaflog.data.local.database.entities.TastingTagEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaTypeEntity
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.UserPreferences

// Stats are left at their empty values: import recomputes them

internal fun TeaTypeEntity.toBackup() = BackupTeaType(
    id = id,
    name = name,
    defaultTemperatureCelsius = defaultTemperatureCelsius,
    colorHex = colorHex,
    isSystemDefault = isSystemDefault,
    displayOrder = displayOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun BackupTeaType.toEntity() = TeaTypeEntity(
    id = id,
    name = name,
    defaultTemperatureCelsius = defaultTemperatureCelsius,
    colorHex = colorHex,
    isSystemDefault = isSystemDefault,
    displayOrder = displayOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun BrewingVesselEntity.toBackup() = BackupVessel(
    id = id,
    name = name,
    iconName = iconName,
    imagePath = imagePath,
    capacityMl = capacityMl,
    isSystemDefault = isSystemDefault,
    isArchived = isArchived,
    displayOrder = displayOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun BackupVessel.toEntity() = BrewingVesselEntity(
    id = id,
    name = name,
    iconName = iconName,
    imagePath = imagePath,
    capacityMl = capacityMl,
    isSystemDefault = isSystemDefault,
    isArchived = isArchived,
    displayOrder = displayOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun TeaEntity.toBackup() = BackupTea(
    id = id,
    name = name,
    teaTypeId = teaTypeId,
    origin = origin,
    producer = producer,
    purchaseDate = purchaseDate,
    defaultTemperatureCelsius = defaultTemperatureCelsius,
    description = description,
    photos = photos,
    isFavorite = isFavorite,
    finishedAt = finishedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun BackupTea.toEntity() = TeaEntity(
    id = id,
    name = name,
    teaTypeId = teaTypeId,
    origin = origin,
    producer = producer,
    purchaseDate = purchaseDate,
    defaultTemperatureCelsius = defaultTemperatureCelsius,
    description = description,
    photos = photos,
    isFavorite = isFavorite,
    finishedAt = finishedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun TeaSessionEntity.toBackup(tags: List<SteepTagEntity>) = BackupSteep(
    id = id,
    teaId = teaId,
    parentSessionId = parentSessionId,
    steepNumber = steepNumber,
    status = status,
    teaQuantityGrams = teaQuantityGrams,
    vesselId = vesselId,
    waterType = waterType,
    location = location,
    planSessionId = planSessionId,
    rinsed = rinsed,
    sessionNotes = sessionNotes,
    rating = rating,
    timestamp = timestamp,
    brewingTime = brewingTime,
    temperatureCelsius = temperatureCelsius,
    waterQuantityMl = waterQuantityMl,
    notes = notes,
    photos = photos,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    timerStatus = timerStatus,
    timerStartedAt = timerStartedAt,
    timerPausedAt = timerPausedAt,
    timerRemainingMs = timerRemainingMs,
    timerTotalMs = timerTotalMs,
    actualBrewingTimeMs = actualBrewingTimeMs,
    tags = tags.map { BackupSteepTag(tagId = it.tagId, confirmed = it.confirmed) },
)

internal fun BackupSteep.toEntity() = TeaSessionEntity(
    id = id,
    teaId = teaId,
    parentSessionId = parentSessionId,
    steepNumber = steepNumber,
    status = status,
    teaQuantityGrams = teaQuantityGrams,
    vesselId = vesselId,
    waterType = waterType,
    location = location,
    planSessionId = planSessionId,
    rinsed = rinsed,
    sessionNotes = sessionNotes,
    rating = rating,
    timestamp = timestamp,
    brewingTime = brewingTime,
    temperatureCelsius = temperatureCelsius,
    waterQuantityMl = waterQuantityMl,
    notes = notes,
    photos = photos,
    userId = null,
    syncStatus = SyncStatus.LOCAL_ONLY,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    timerStatus = timerStatus,
    timerStartedAt = timerStartedAt,
    timerPausedAt = timerPausedAt,
    timerRemainingMs = timerRemainingMs,
    timerTotalMs = timerTotalMs,
    actualBrewingTimeMs = actualBrewingTimeMs,
)

internal fun UserPreferences.toBackup() = BackupPreferences(
    temperatureUnit = temperatureUnit,
    volumeUnit = volumeUnit,
    weightUnit = weightUnit,
    teaSortOption = teaSortOption,
    analyticsPeriod = analyticsPeriod.name,
    defaultWaterType = defaultWaterType,
    timerCompletionNotificationsEnabled = timerCompletionNotificationsEnabled,
    sessionReminderNotificationsEnabled = sessionReminderNotificationsEnabled,
    themeMode = themeMode,
    hemisphere = hemisphere,
)

internal fun BackupPreferences.toDomain() = UserPreferences(
    temperatureUnit = temperatureUnit,
    volumeUnit = volumeUnit,
    weightUnit = weightUnit,
    teaSortOption = teaSortOption,
    analyticsPeriod = AnalyticsPeriod.named(analyticsPeriod),
    defaultWaterType = defaultWaterType,
    timerCompletionNotificationsEnabled = timerCompletionNotificationsEnabled,
    sessionReminderNotificationsEnabled = sessionReminderNotificationsEnabled,
    themeMode = themeMode,
    hemisphere = hemisphere,
)

internal fun BackupSteep.tagEntities() =
    tags.map { SteepTagEntity(steepId = id, tagId = it.tagId, confirmed = it.confirmed) }

internal fun TastingTagEntity.toBackup() = BackupTastingTag(
    id = id,
    key = key,
    name = name,
    isCustom = isCustom,
    displayOrder = displayOrder,
    deletedAt = deletedAt,
)

internal fun BackupTastingTag.toEntity() = TastingTagEntity(
    id = id,
    key = key,
    name = name,
    isCustom = isCustom,
    displayOrder = displayOrder,
    deletedAt = deletedAt,
)
