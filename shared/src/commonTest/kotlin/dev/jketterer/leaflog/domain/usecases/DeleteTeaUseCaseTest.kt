package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
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

class DeleteTeaUseCaseTest {

    private val sessions = FakeTeaSessionRepository()
    private val teas = FakeTeaRepository(listOf(TEA))
    private val transactor = FakeTransactor()

    private val deleteTea = DeleteTeaUseCase(transactor, teas, sessions)
    private val startSession = StartSessionUseCase(
        transactor,
        sessions,
        teas,
        RecomputeStatsUseCase(sessions, teas),
        Clock.System,
    )

    @Test
    fun `a tea never brewed is deleted`() = runTest {
        deleteTea(TEA.id).getOrThrow()

        assertNull(teas.getById(TEA.id))
    }

    @Test
    fun `a tea with only an in-progress session is refused and kept`() = runTest {
        startSession(
            teaId = TEA.id,
            vesselId = "gaiwan",
            waterType = WaterType.FILTERED,
            brewingTime = 20.seconds,
            temperatureCelsius = 95.0,
            waterQuantityMl = 100.0,
        ).getOrThrow()

        val result = deleteTea(TEA.id)

        val failure = assertIs<DomainFailure.InUse>(result.exceptionOrNull())
        assertEquals(1, failure.count)
        assertNotNull(teas.getById(TEA.id))
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
    }
}
