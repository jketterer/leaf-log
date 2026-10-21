package dev.jketterer.leaflog.data.mappers

import dev.jketterer.leaflog.data.local.database.entities.SteepRow
import dev.jketterer.leaflog.data.local.database.entities.SteepTagEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SteepTag
import kotlin.time.Duration.Companion.milliseconds

/**
 * Groups parent rows with their steeps. Sessions keep the order their parent rows came in;
 * children whose parent isn't in the list are dropped. Callers pass live rows only.
 */
fun List<SteepRow>.toAggregates(): List<SessionWithSteeps> {
    val childrenByParent = filter { it.steep.parentSessionId != null }.groupBy { it.steep.parentSessionId }
    return filter { it.steep.parentSessionId == null }.map { parent ->
        SessionWithSteeps(
            session = parent.steep.toSession(),
            steeps = (listOf(parent) + childrenByParent[parent.steep.id].orEmpty())
                .sortedBy { it.steep.steepNumber }
                .map { it.steep.toSteep(it.tags) },
        )
    }
}

private fun TeaSessionEntity.toSession() = Session(
    id = id,
    teaId = teaId,
    status = status,
    vesselId = checkNotNull(vesselId) { "Session $id has no vessel" },
    waterType = checkNotNull(waterType) { "Session $id has no water type" },
    teaQuantityGrams = teaQuantityGrams,
    location = location,
    usedConfigurationId = usedConfigurationId,
    planSessionId = planSessionId,
    rinsed = rinsed,
    notes = sessionNotes,
    userId = userId,
    syncStatus = syncStatus,
)

private fun TeaSessionEntity.toSteep(tags: List<SteepTagEntity>) = Steep(
    id = id,
    number = steepNumber,
    timestamp = timestamp,
    brewingTime = brewingTime,
    temperatureCelsius = temperatureCelsius,
    waterQuantityMl = waterQuantityMl,
    rating = rating,
    notes = notes,
    photos = photos,
    tags = tags.map { SteepTag(it.tagId, it.confirmed) },
    createdAt = createdAt,
    updatedAt = updatedAt,
    timerStatus = timerStatus,
    timerStartedAt = timerStartedAt,
    timerPausedAt = timerPausedAt,
    timerRemainingMs = timerRemainingMs,
    timerTotalMs = timerTotalMs,
    actualTime = actualBrewingTimeMs?.milliseconds,
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
        teaQuantityGrams = session.teaQuantityGrams.takeIf { isFirst },
        vesselId = session.vesselId.takeIf { isFirst },
        waterType = session.waterType.takeIf { isFirst },
        location = session.location.takeIf { isFirst },
        usedConfigurationId = session.usedConfigurationId.takeIf { isFirst },
        planSessionId = session.planSessionId.takeIf { isFirst },
        rinsed = session.rinsed && isFirst,
        sessionNotes = session.notes.takeIf { isFirst },
        rating = rating,
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
        actualBrewingTimeMs = actualTime?.inWholeMilliseconds,
    )
}

/** This steep's row and its tags. */
fun Steep.toRow(session: Session) = SteepRow(
    steep = toEntity(session),
    tags = tags.map { SteepTagEntity(steepId = id, tagId = it.tagId, confirmed = it.confirmed) },
)
