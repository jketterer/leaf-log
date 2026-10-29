package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.FakeBrewingVesselRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTeaRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTransactor
import dev.jketterer.leaflog.domain.usecases.session.RecomputeStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DeleteBrewingVesselUseCaseTest {

    private val sessions = FakeTeaSessionRepository()
    private val teas = FakeTeaRepository(listOf(TEA))
    private val vessels = FakeBrewingVesselRepository(listOf(vessel("gaiwan"), vessel("mug")))
    private val transactor = FakeTransactor()

    private val deleteVessel = DeleteBrewingVesselUseCase(transactor, vessels, sessions)
    private val startSession = StartSessionUseCase(
        transactor,
        sessions,
        teas,
        RecomputeStatsUseCase(sessions, teas),
        Clock.System,
    )

    @Test
    fun `an unused vessel is deleted`() = runTest {
        deleteVessel("gaiwan").getOrThrow()

        assertNull(vessels.getById("gaiwan"))
    }

    @Test
    fun `a vessel a session used is refused and kept`() = runTest {
        startSession(
            teaId = TEA.id,
            vesselId = "gaiwan",
            waterType = WaterType.FILTERED,
            brewingTime = 20.seconds,
            temperatureCelsius = 95.0,
            waterQuantityMl = 100.0,
        ).getOrThrow()

        val result = deleteVessel("gaiwan")

        val failure = assertIs<DomainFailure.InUse>(result.exceptionOrNull())
        assertEquals(1, failure.count)
        assertNotNull(vessels.getById("gaiwan"))
    }

    @Test
    fun `the last active vessel is refused`() = runTest {
        deleteVessel("gaiwan").getOrThrow()

        val result = deleteVessel("mug")

        assertIs<DomainFailure.LastRemaining>(result.exceptionOrNull())
        assertNotNull(vessels.getById("mug"))
    }

    private companion object {
        val EPOCH = Instant.fromEpochMilliseconds(0)

        val TEA = Tea(
            id = "tea",
            name = "Tieguanyin",
            teaTypeId = "oolong",
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )

        fun vessel(id: String) = BrewingVessel(
            id = id,
            name = id,
            iconName = null,
            isSystemDefault = false,
            displayOrder = 0,
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )
    }
}
