package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TimedSteep
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class AttachTeaUseCaseTest {

    private val sessions = FakeTeaSessionRepository()
    private val teas = FakeTeaRepository(listOf(TEA))
    private val transactor = FakeTransactor()
    private val recomputeStats = RecomputeStatsUseCase(sessions, teas)
    private val startSession = StartSessionUseCase(transactor, sessions, teas, recomputeStats, Clock.System)
    private val attachTea = AttachTeaUseCase(
        transactor = transactor,
        teaSessionRepository = sessions,
        teaRepository = teas,
        brewingVesselRepository = FakeBrewingVesselRepository(listOf(GAIWAN, MUG)),
        getBrewingParametersPrefill = GetBrewingParametersPrefillUseCase(
            FakeTeaTypeRepository(listOf(OOLONG)),
            sessions,
        ),
        recomputeStats = recomputeStats,
        clock = Clock.System,
    )

    @Test
    fun `attaching after three steeps logs all three unrated at their real times`() = runTest {
        // Steep 3 has just finished; the timer brewed it a moment past its 30s
        val timer = TimerState(
            steepNumber = 3,
            totalDuration = 30.4.seconds,
            status = TimerStatus.COMPLETE,
            startedAt = T0 + 2.minutes,
            finishedSteeps = listOf(
                TimedSteep(T0, 20.seconds),
                TimedSteep(T0 + 1.minutes, 25.seconds),
            ),
        )

        val session = attachTea(TEA.id, timer, WaterType.FILTERED).getOrThrow()

        assertEquals(listOf(1, 2, 3), session.steeps.map { it.number })
        assertEquals(listOf(T0, T0 + 1.minutes, T0 + 2.minutes), session.steeps.map { it.timestamp })
        assertEquals(listOf(20.seconds, 25.seconds, 30.seconds), session.steeps.map { it.actualTime })
        assertEquals(T0, session.startedAt)
        assertNull(session.effectiveRating)
        assertEquals(SessionStatus.IN_PROGRESS, session.session.status)
        assertEquals(session, sessions.getSession(session.session.id))
    }

    @Test
    fun `a steep still brewing keeps its timer`() = runTest {
        val timer = TimerState(
            steepNumber = 2,
            totalDuration = 25.seconds,
            remainingDuration = 10.seconds,
            status = TimerStatus.RUNNING,
            startedAt = T0 + 1.minutes,
            finishedSteeps = listOf(TimedSteep(T0, 20.seconds)),
        )

        val current = attachTea(TEA.id, timer, WaterType.FILTERED).getOrThrow().currentSteep

        assertEquals(2, current.number)
        assertEquals(25.seconds, current.brewingTime)
        assertEquals(TimerStatus.RUNNING, current.timerStatus)
        assertEquals(T0 + 1.minutes, current.timerStartedAt)
        assertNull(current.actualTime)
    }

    @Test
    fun `the vessel is the one last used with the tea and brings its prefill`() = runTest {
        startSession(
            teaId = TEA.id,
            vesselId = MUG.id,
            waterType = WaterType.SPRING,
            brewingTime = 3.minutes,
            temperatureCelsius = 90.0,
            waterQuantityMl = 300.0,
            status = SessionStatus.COMPLETED,
            rating = 4f,
        ).getOrThrow()

        val session = attachTea(TEA.id, RUNNING, WaterType.FILTERED).getOrThrow()

        assertEquals(MUG.id, session.session.vesselId)
        assertEquals(WaterType.SPRING, session.session.waterType)
        assertEquals(90.0, session.currentSteep.temperatureCelsius)
        assertEquals(300.0, session.currentSteep.waterQuantityMl)
    }

    @Test
    fun `with no history it uses the type's temperature and the vessel's capacity`() = runTest {
        val session = attachTea(TEA.id, RUNNING, WaterType.FILTERED).getOrThrow()

        assertEquals(GAIWAN.id, session.session.vesselId)
        assertEquals(WaterType.FILTERED, session.session.waterType)
        assertEquals(OOLONG.defaultTemperatureCelsius, session.currentSteep.temperatureCelsius)
        assertEquals(120.0, session.currentSteep.waterQuantityMl)
    }

    @Test
    fun `next by feel brews 5s longer than the last steep did and waits for Start`() {
        val done = RUNNING.copy(status = TimerStatus.COMPLETE, totalDuration = 20.6.seconds)

        val next = done.nextByFeel()

        assertEquals(2, next.steepNumber)
        assertEquals(26.seconds, next.totalDuration)
        assertEquals(TimerStatus.NOT_STARTED, next.status)
        assertEquals(listOf(TimedSteep(T0, 21.seconds)), next.finishedSteeps)
    }

    private companion object {
        val T0 = Instant.fromEpochMilliseconds(1_000_000)
        val EPOCH = Instant.fromEpochMilliseconds(0)

        val RUNNING = TimerState(
            totalDuration = 20.seconds,
            remainingDuration = 15.seconds,
            status = TimerStatus.RUNNING,
            startedAt = T0,
        )

        val OOLONG = TeaType(
            id = "oolong",
            name = "Oolong",
            defaultTemperatureCelsius = 95.0,
            colorHex = "#FF9800",
            isSystemDefault = true,
            displayOrder = 0,
        )

        val TEA = Tea(
            id = "tea",
            name = "Tieguanyin",
            teaTypeId = OOLONG.id,
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )

        val GAIWAN = vessel("gaiwan", capacityMl = 120)
        val MUG = vessel("mug", capacityMl = 350)

        fun vessel(id: String, capacityMl: Int) = BrewingVessel(
            id = id,
            name = id,
            iconName = null,
            capacityMl = capacityMl,
            isSystemDefault = true,
            displayOrder = 0,
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )
    }
}
