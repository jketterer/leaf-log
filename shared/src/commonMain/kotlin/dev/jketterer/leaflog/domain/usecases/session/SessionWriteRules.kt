package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import kotlin.time.Duration
import kotlin.time.Instant

internal fun Session.requireValid() {
    require(vesselId.isNotBlank()) { "Brewing vessel must be selected" }
    require(teaQuantityGrams == null || teaQuantityGrams > 0) { "Tea quantity must be greater than 0" }
}

internal fun Steep.requireValid() {
    require(temperatureCelsius in 0.0..100.0) { "Temperature must be between 0°C and 100°C" }
    require(waterQuantityMl > 0) { "Water quantity must be greater than 0" }
    require(brewingTime > Duration.ZERO) { "Brewing time must be greater than 0" }
    require(rating == null || rating in 0f..5f) { "Rating must be between 0 and 5" }
}

internal fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** Session fields are stored on steep 1's row, so changing them dates that row. */
internal fun SessionWithSteeps.withSession(session: Session, now: Instant) = copy(
    session = session,
    steeps = steeps.map { if (it.number == 1) it.copy(updatedAt = now) else it },
)
