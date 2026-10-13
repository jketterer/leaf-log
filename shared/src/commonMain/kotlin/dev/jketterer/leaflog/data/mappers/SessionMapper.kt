package dev.jketterer.leaflog.data.mappers

import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep

/**
 * Groups parent rows with their steeps. Sessions keep the order their parent rows came in;
 * children whose parent isn't in the list are dropped. Callers pass live rows only.
 */
fun List<TeaSessionEntity>.toAggregates(): List<SessionWithSteeps> {
    val childrenByParent = filter { it.parentSessionId != null }.groupBy { it.parentSessionId }
    return filter { it.parentSessionId == null }.map { parent ->
        SessionWithSteeps(
            session = parent.toSession(),
            steeps = (listOf(parent) + childrenByParent[parent.id].orEmpty())
                .sortedBy { it.steepNumber }
                .map { it.toSteep() },
        )
    }
}

private fun TeaSessionEntity.toSession() = Session(
    id = id,
    teaId = teaId,
    status = status,
    vesselId = vesselId,
    waterType = waterType,
    teaQuantityGrams = teaQuantityGrams,
    location = location,
    usedConfigurationId = usedConfigurationId,
    userId = userId,
    syncStatus = syncStatus,
)

private fun TeaSessionEntity.toSteep() = Steep(
    id = id,
    number = steepNumber,
    timestamp = timestamp,
    brewingTime = brewingTime,
    temperatureCelsius = temperatureCelsius,
    waterQuantityMl = waterQuantityMl,
    rating = rating,
    notes = notes,
    photos = photos,
    createdAt = createdAt,
    updatedAt = updatedAt,
    timerStatus = timerStatus,
    timerStartedAt = timerStartedAt,
    timerPausedAt = timerPausedAt,
    timerRemainingMs = timerRemainingMs,
    timerTotalMs = timerTotalMs,
)

/** The row for this steep. Steep 1 is the session row; the rest point at it. */
fun Steep.toEntity(session: Session): TeaSessionEntity {
    val isFirst = id == session.id
    return TeaSessionEntity(
        id = id,
        teaId = session.teaId,
        parentSessionId = if (isFirst) null else session.id,
        steepNumber = number,
        status = session.status,
        // Until schema v7 makes them nullable, children carry the session's columns too
        teaQuantityGrams = session.teaQuantityGrams,
        vesselId = session.vesselId,
        waterType = session.waterType,
        location = session.location,
        usedConfigurationId = session.usedConfigurationId,
        rating = rating,
        averageRating = null,
        timestamp = timestamp,
        brewingTime = brewingTime,
        temperatureCelsius = temperatureCelsius,
        waterQuantityMl = waterQuantityMl,
        notes = notes,
        photos = photos,
        userId = session.userId,
        syncStatus = session.syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = null,
        timerStatus = timerStatus,
        timerStartedAt = timerStartedAt,
        timerPausedAt = timerPausedAt,
        timerRemainingMs = timerRemainingMs,
        timerTotalMs = timerTotalMs,
    )
}
