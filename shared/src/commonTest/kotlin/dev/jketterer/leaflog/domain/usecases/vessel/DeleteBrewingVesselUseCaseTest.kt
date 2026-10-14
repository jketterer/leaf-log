package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.FakeBrewingConfigurationRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeBrewingVesselRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTeaRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTeaSessionRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTransactor
import dev.jketterer.leaflog.domain.usecases.session.RecomputeStatsUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DeleteBrewingVesselUseCaseTest {

    private val sessions = FakeTeaSessionRepository()
    private val teas = FakeTeaRepository(listOf(TEA))
    private val vessels = FakeBrewingVesselRepository(listOf(vessel("gaiwan"), vessel("mug")))
    private val configurations = FakeBrewingConfigurationRepository(listOf(METHOD))
    private val transactor = FakeTransactor()

    private val deleteVessel = DeleteBrewingVesselUseCase(
        transactor,
        vessels,
        sessions,
        configurations,
    )
    private val startSession = StartSessionUseCase(
        transactor,
        sessions,
        RecomputeStatsUseCase(sessions, teas, configurations),
    )

    @Test
    fun `an unused vessel is deleted with its methods`() = runTest {
        deleteVessel("gaiwan").getOrThrow()

        assertNull(vessels.getById("gaiwan"))
        assertNull(configurations.getById(METHOD.id))
    }

    @Test
    fun `a vessel a session used is refused and its methods kept`() = runTest {
        startSession(METHOD).getOrThrow()

        val result = deleteVessel("gaiwan")

        assertTrue(result.isFailure)
        assertNotNull(vessels.getById("gaiwan"))
        assertNotNull(configurations.getById(METHOD.id))
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

        val METHOD = BrewingConfiguration(
            id = "method",
            teaId = TEA.id,
            vesselId = "gaiwan",
            teaQuantityGrams = 5f,
            waterQuantityMl = 100.0,
            temperatureCelsius = 95.0,
            brewingTime = 20.seconds,
            waterType = WaterType.FILTERED,
            sourceSessionId = null,
            rating = null,
            timesUsed = 0,
            lastUsedAt = null,
            label = null,
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
