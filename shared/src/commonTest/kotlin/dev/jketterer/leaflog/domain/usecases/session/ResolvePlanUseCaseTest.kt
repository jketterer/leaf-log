package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ResolvePlanUseCaseTest {

    @Test
    fun `a session follows its method before anything else`() = runTest {
        val own = session("own", 20, 25, methodId = "m1", planSessionId = "source")
        val resolve = resolver(methods = listOf(method("m1", increment = 10)), sessions = listOf(session("source", 60)))

        assertEquals(30.seconds, resolve(own).timeOf(3))
    }

    @Test
    fun `without a method it follows the session brew again copied`() = runTest {
        val own = session("own", 60, planSessionId = "source")
        val resolve = resolver(sessions = listOf(session("source", 20, 30)))

        assertEquals(40.seconds, resolve(own).timeOf(3))
    }

    @Test
    fun `without either it carries on by feel from its own steeps`() = runTest {
        val resolve = resolver()

        assertEquals(30.seconds, resolve(session("own", 20, 25)).timeOf(3))
        assertEquals(60.seconds, resolve(session("single", 60)).timeOf(2))
    }

    @Test
    fun `a method or source that is gone is skipped`() = runTest {
        val own = session("own", 20, 25, methodId = "deleted", planSessionId = "deleted-too")

        assertEquals(30.seconds, resolver()(own).timeOf(3))
    }

    private fun resolver(
        methods: List<BrewingConfiguration> = emptyList(),
        sessions: List<SessionWithSteeps> = emptyList(),
    ) = ResolvePlanUseCase(FakeBrewingConfigurationRepository(methods), FakeTeaSessionRepository(sessions))

    private fun session(
        id: String,
        vararg seconds: Int,
        methodId: String? = null,
        planSessionId: String? = null,
    ) = SessionWithSteeps(
        session = Session(
            id = id,
            teaId = "tea",
            status = SessionStatus.IN_PROGRESS,
            vesselId = "gaiwan",
            waterType = WaterType.FILTERED,
            usedConfigurationId = methodId,
            planSessionId = planSessionId,
            syncStatus = SyncStatus.LOCAL_ONLY,
        ),
        steeps = seconds.mapIndexed { i, time ->
            Steep(
                id = if (i == 0) id else "$id-${i + 1}",
                number = i + 1,
                timestamp = AT,
                brewingTime = time.seconds,
                temperatureCelsius = 95.0,
                waterQuantityMl = 100.0,
                createdAt = AT,
                updatedAt = AT,
            )
        },
    )

    private fun method(id: String, increment: Int) = BrewingConfiguration(
        id = id,
        teaId = "tea",
        vesselId = "gaiwan",
        teaQuantityGrams = 5f,
        waterQuantityMl = 100.0,
        temperatureCelsius = 95.0,
        brewingTime = 10.seconds,
        steepIncrement = increment.seconds,
        waterType = WaterType.FILTERED,
        sourceSessionId = null,
        rating = null,
        timesUsed = 0,
        lastUsedAt = null,
        label = null,
        createdAt = AT,
        updatedAt = AT,
    )

    private companion object {
        val AT = Instant.fromEpochSeconds(0)
    }
}
