package dev.jketterer.leaflog.data.backup

import dev.jketterer.leaflog.data.local.database.entities.BrewingConfigurationEntity
import dev.jketterer.leaflog.data.local.database.entities.BrewingVesselEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaTypeEntity
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

internal fun TeaSessionEntity.toBackup() = BackupSteep(
    id = id,
    teaId = teaId,
    parentSessionId = parentSessionId,
    steepNumber = steepNumber,
    status = status,
    teaQuantityGrams = teaQuantityGrams,
    vesselId = vesselId,
    waterType = waterType,
    location = location,
    usedConfigurationId = usedConfigurationId,
    planSessionId = planSessionId,
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
    usedConfigurationId = usedConfigurationId,
    planSessionId = planSessionId,
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

internal fun BrewingConfigurationEntity.toBackup() = BackupMethod(
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
    sourceSessionId = sourceSessionId,
    rating = rating,
    label = label,
    isPinned = isPinned,
    pinnedSortOrder = pinnedSortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun BackupMethod.toEntity() = BrewingConfigurationEntity(
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
    sourceSessionId = sourceSessionId,
    rating = rating,
    timesUsed = 0,
    lastUsedAt = null,
    label = label,
    isPinned = isPinned,
    pinnedSortOrder = pinnedSortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun UserPreferences.toBackup() = BackupPreferences(
    temperatureUnit = temperatureUnit,
    volumeUnit = volumeUnit,
    weightUnit = weightUnit,
    teaSortOption = teaSortOption,
    analyticsPeriod = analyticsPeriod,
    defaultWaterType = defaultWaterType,
    timerCompletionNotificationsEnabled = timerCompletionNotificationsEnabled,
    sessionReminderNotificationsEnabled = sessionReminderNotificationsEnabled,
    themeMode = themeMode,
)

internal fun BackupPreferences.toDomain() = UserPreferences(
    temperatureUnit = temperatureUnit,
    volumeUnit = volumeUnit,
    weightUnit = weightUnit,
    teaSortOption = teaSortOption,
    analyticsPeriod = analyticsPeriod,
    defaultWaterType = defaultWaterType,
    timerCompletionNotificationsEnabled = timerCompletionNotificationsEnabled,
    sessionReminderNotificationsEnabled = sessionReminderNotificationsEnabled,
    themeMode = themeMode,
)
