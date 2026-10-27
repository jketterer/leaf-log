package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TimedSteep
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import dev.jketterer.leaflog.domain.repositories.Transactor
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Turns a timer with no session into a session of the tea: every steep it finished at its real
 * start and length, unrated, then the steep it's timing now, which keeps its timer. The session
 * starts when its first steep did.
 *
 * Only the tea is asked for, so the rest is a guess the review's edit sheet fixes: the vessel last
 * used with the tea, else the most used; Log Tea's prefill for the pair, else the vessel's
 * capacity and [defaultWaterType].
 */
class AttachTeaUseCase(
    private val transactor: Transactor,
    private val teaSessionRepository: TeaSessionRepository,
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val getBrewingParametersPrefill: GetBrewingParametersPrefillUseCase,
    private val recomputeStats: RecomputeStatsUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        teaId: String,
        timer: TimerState,
        defaultWaterType: WaterType,
    ): Result<SessionWithSteeps> = runCatching {
        require(timer.sessionId == null) { "The timer already has a session" }
        val tea = teaRepository.getById(teaId) ?: throw DomainFailure.NotFound("tea $teaId")
        val vessel = chooseVessel(teaId) ?: throw DomainFailure.NotFound("a brewing vessel")
        val prefill = getBrewingParametersPrefill(tea, vessel)
        val temperatureCelsius = prefill.temperatureCelsius ?: FALLBACK_TEMPERATURE_CELSIUS
        val waterQuantityMl = prefill.waterQuantityMl
            ?: vessel.capacityMl?.toDouble()
            ?: FALLBACK_WATER_ML

        val now = clock.now()
        val id = Uuid.random().toString()
        fun steep(number: Int) = Steep(
            id = if (number == 1) id else Uuid.random().toString(),
            number = number,
            timestamp = now,
            brewingTime = timer.totalDuration,
            temperatureCelsius = temperatureCelsius,
            waterQuantityMl = waterQuantityMl,
            createdAt = now,
            updatedAt = now,
        )

        fun Steep.finished(brewed: TimedSteep) = copy(
            timestamp = brewed.startedAt,
            brewingTime = brewed.length,
            actualTime = brewed.length,
            timerStatus = TimerStatus.COMPLETE,
            timerStartedAt = brewed.startedAt,
            timerRemainingMs = 0,
            timerTotalMs = brewed.length.inWholeMilliseconds,
        )

        val finished =
            timer.finishedSteeps.mapIndexed { i, brewed -> steep(i + 1).finished(brewed) }
        val currentNumber = finished.size + 1
        val brewed = timer.brewed
        val current = when {
            brewed != null -> steep(currentNumber).finished(brewed)
            timer.status == TimerStatus.NOT_STARTED -> steep(currentNumber)
            else -> steep(currentNumber).copy(
                timestamp = timer.startedAt ?: now,
                timerStatus = timer.status,
                timerStartedAt = timer.startedAt,
                timerPausedAt = timer.pausedAt,
                timerRemainingMs = timer.remainingDuration.inWholeMilliseconds,
                timerTotalMs = timer.totalDuration.inWholeMilliseconds,
            )
        }

        val session = SessionWithSteeps(
            session = Session(
                id = id,
                teaId = teaId,
                status = SessionStatus.IN_PROGRESS,
                vesselId = vessel.id,
                waterType = prefill.waterType ?: defaultWaterType,
                teaQuantityGrams = prefill.teaQuantityGrams,
                syncStatus = SyncStatus.LOCAL_ONLY,
            ),
            steeps = finished + current,
        )
        session.session.requireValid()
        session.steeps.forEach { it.requireValid() }

        transactor {
            teaSessionRepository.saveSession(session)
            teaRepository.setFinished(teaId, finished = false)
            recomputeStats(teaId)
        }
        session
    }

    // reads every session to rank vessels; a counting query if history grows large
    private suspend fun chooseVessel(teaId: String): BrewingVessel? {
        val active = brewingVesselRepository.getActive().associateBy { it.id }
        val lastWithTea = teaSessionRepository.getSessionsByTea(teaId)
            .filter { it.session.vesselId in active }
            .maxByOrNull { it.startedAt }
            ?.session?.vesselId
        val mostUsed = teaSessionRepository.getAllSessions()
            .groupingBy { it.session.vesselId }.eachCount()
            .filterKeys { it in active }
            .maxByOrNull { it.value }?.key
        return (lastWithTea ?: mostUsed)?.let(active::get) ?: active.values.firstOrNull()
    }

    private companion object {
        // Nothing knew better: boiling, and a small cup
        const val FALLBACK_TEMPERATURE_CELSIUS = 100.0
        const val FALLBACK_WATER_ML = 100.0
    }
}
