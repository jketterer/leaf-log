package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.FakeBrewingConfigurationRepository
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

class DeleteTeaUseCaseTest {

    private val sessions = FakeTeaSessionRepository()
    private val teas = FakeTeaRepository(listOf(TEA))
    private val configurations = FakeBrewingConfigurationRepository(listOf(METHOD))
    private val transactor = FakeTransactor()

    private val deleteTea = DeleteTeaUseCase(transactor, teas, sessions, configurations)
    private val startSession = StartSessionUseCase(
        transactor,
        sessions,
        RecomputeStatsUseCase(sessions, teas, configurations),
    )

    @Test
    fun `a tea never brewed is deleted with its methods`() = runTest {
        deleteTea(TEA.id).getOrThrow()

        assertNull(teas.getById(TEA.id))
        assertNull(configurations.getById(METHOD.id))
    }

    @Test
    fun `a tea with only an in-progress session is refused and kept`() = runTest {
        startSession(METHOD).getOrThrow()

        val result = deleteTea(TEA.id)

        assertTrue(result.isFailure)
        assertNotNull(teas.getById(TEA.id))
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
    }
}
