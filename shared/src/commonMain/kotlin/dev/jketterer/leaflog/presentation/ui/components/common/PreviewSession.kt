package dev.jketterer.leaflog.presentation.ui.components.common

import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** A session for @Preview functions, with one steep per entry of [ratings]. */
internal fun previewSession(
    id: String = "session-1",
    ratings: List<Float?> = listOf(null),
    status: SessionStatus = SessionStatus.IN_PROGRESS,
    brewingTime: Duration = 2.minutes,
    temperatureCelsius: Double = 80.0,
    waterQuantityMl: Double = 200.0,
    teaQuantityGrams: Float? = 5f,
): SessionWithSteeps {
    val start = Instant.fromEpochSeconds(1_790_000_000L)
    return SessionWithSteeps(
        session = Session(
            id = id,
            teaId = "tea-1",
            status = status,
            vesselId = "vessel-1",
            waterType = WaterType.FILTERED,
            teaQuantityGrams = teaQuantityGrams,
            syncStatus = SyncStatus.LOCAL_ONLY,
        ),
        steeps = ratings.mapIndexed { index, rating ->
            val timestamp = start + brewingTime * index
            Steep(
                id = if (index == 0) id else "$id-${index + 1}",
                number = index + 1,
                timestamp = timestamp,
                brewingTime = brewingTime,
                temperatureCelsius = temperatureCelsius,
                waterQuantityMl = waterQuantityMl,
                rating = rating,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        },
    )
}
