package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.uuid.Uuid

/**
 * Creates a session and its first steep. Pass [SessionStatus.COMPLETED] to log one after the fact.
 * [notes] is the session's note; [rating] and [photos] go on steep 1.
 * Brewing a finished tea puts it back in stock.
 */
class StartSessionUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val recomputeStats: RecomputeStatsUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        teaId: String,
        vesselId: String,
        waterType: WaterType,
        brewingTime: Duration,
        temperatureCelsius: Double,
        waterQuantityMl: Double,
        teaQuantityGrams: Float? = null,
        location: String? = null,
        notes: String? = null,
        rating: Float? = null,
        photos: List<String> = emptyList(),
        status: SessionStatus = SessionStatus.IN_PROGRESS,
        planSessionId: String? = null,
        rinsed: Boolean = false,
    ): Result<SessionWithSteeps> = runCatching {
        require(teaId.isNotBlank()) { "Tea must be selected" }

        val now = clock.now()
        val id = Uuid.random().toString()
        val started = SessionWithSteeps(
            session = Session(
                id = id,
                teaId = teaId,
                status = status,
                vesselId = vesselId,
                waterType = waterType,
                teaQuantityGrams = teaQuantityGrams,
                location = location.blankToNull(),
                planSessionId = planSessionId,
                rinsed = rinsed,
                notes = notes.blankToNull(),
                syncStatus = SyncStatus.LOCAL_ONLY,
            ),
            steeps = listOf(
                Steep(
                    id = id,
                    number = 1,
                    timestamp = now,
                    brewingTime = brewingTime,
                    temperatureCelsius = temperatureCelsius,
                    waterQuantityMl = waterQuantityMl,
                    rating = rating,
                    photos = photos,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
        )
        started.session.requireValid()
        started.steeps.single().requireValid()

        transactor {
            teaSessionRepository.saveSession(started)
            teaRepository.setFinished(teaId, finished = false)
            recomputeStats(teaId)
        }
        started
    }

    /**
     * Brew Again: [source]'s set-up, with nothing it recorded about how it went. Its steeps become
     * this session's plan, so every steep replays as brewed rather than only the first.
     */
    suspend operator fun invoke(source: SessionWithSteeps): Result<SessionWithSteeps> {
        val first = source.steeps.first()
        return invoke(
            teaId = source.session.teaId,
            vesselId = source.session.vesselId,
            waterType = source.session.waterType,
            brewingTime = BrewingPlan.learnedFrom(source.steeps).base,
            temperatureCelsius = first.temperatureCelsius,
            waterQuantityMl = first.waterQuantityMl,
            teaQuantityGrams = source.session.teaQuantityGrams,
            location = source.session.location,
            planSessionId = source.session.id,
            rinsed = source.session.rinsed,
        )
    }
}
