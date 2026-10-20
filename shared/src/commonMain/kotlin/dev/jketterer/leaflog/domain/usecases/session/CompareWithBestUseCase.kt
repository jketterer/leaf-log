package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlin.math.abs

/** What the brewing differs in from another session; the screen formats both sides. */
enum class BrewDifference { LEAF, WATER, TEMPERATURE, TIME, VESSEL, WATER_TYPE }

sealed interface VsBest {
    /** Rated higher than every other rated session of the tea. */
    data object NewBest : VsBest

    data class Versus(val best: SessionWithSteeps, val differences: List<BrewDifference>) : VsBest
}

/** Compares a session with the best-rated other completed session of its tea, when both are rated. */
class CompareWithBestUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(session: SessionWithSteeps): VsBest? =
        compareWithBest(session, teaSessionRepository.getSessionsByTea(session.session.teaId))
}

/** Ties for best go to the most recent. Measurement noise (±1 °C, ±10 ml, ±0.5 g, ±5 s) is not a difference. */
fun compareWithBest(session: SessionWithSteeps, teaSessions: List<SessionWithSteeps>): VsBest? {
    val rating = session.effectiveRating ?: return null
    val best = teaSessions
        .filter { it.session.id != session.session.id && it.session.status == SessionStatus.COMPLETED }
        .filter { it.effectiveRating != null }
        .maxWithOrNull(compareBy({ it.effectiveRating }, { it.startedAt }))
        ?: return null
    if (rating > best.effectiveRating!!) return VsBest.NewBest

    val mine = session.steeps.first()
    val theirs = best.steeps.first()
    val myLeaf = session.session.teaQuantityGrams
    val theirLeaf = best.session.teaQuantityGrams
    return VsBest.Versus(
        best = best,
        differences = buildList {
            if ((myLeaf == null) != (theirLeaf == null) ||
                (myLeaf != null && theirLeaf != null && abs(myLeaf - theirLeaf) > 0.5f)
            ) add(BrewDifference.LEAF)
            if (abs(mine.waterQuantityMl - theirs.waterQuantityMl) > 10.0) add(BrewDifference.WATER)
            if (abs(mine.temperatureCelsius - theirs.temperatureCelsius) > 1.0) add(BrewDifference.TEMPERATURE)
            if (abs(mine.brewingTime.inWholeSeconds - theirs.brewingTime.inWholeSeconds) > 5) add(BrewDifference.TIME)
            if (session.session.vesselId != best.session.vesselId) add(BrewDifference.VESSEL)
            if (session.session.waterType != best.session.waterType) add(BrewDifference.WATER_TYPE)
        },
    )
}
