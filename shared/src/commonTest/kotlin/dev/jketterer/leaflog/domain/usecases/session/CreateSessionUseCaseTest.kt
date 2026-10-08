package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class CreateSessionUseCaseTest {

    private val sessionRepository = FakeTeaSessionRepository()

    // incrementTimesUsed throws in the fake, so any usage count at creation fails the test
    private val createSession = CreateSessionUseCase(
        teaSessionRepository = sessionRepository,
        brewingConfigurationRepository = FakeBrewingConfigurationRepository(),
        updateTeaStatsUseCase = UpdateTeaStatsUseCase(sessionRepository, FakeTeaRepository()),
    )

    @Test
    fun `quick brew leaves the session unrated and remembers the method used`() = runTest {
        val session = createSession(CONFIG).getOrThrow()

        assertNull(session.rating)
        assertEquals(CONFIG.id, session.usedConfigurationId)
        assertEquals(SessionStatus.IN_PROGRESS, session.status)
    }

    private companion object {
        val EPOCH = Instant.fromEpochMilliseconds(0)

        val CONFIG = BrewingConfiguration(
            id = "config-1",
            teaId = "tea-1",
            vesselId = "vessel-1",
            teaQuantityGrams = 5f,
            waterQuantityMl = 150.0,
            temperatureCelsius = 95.0,
            brewingTime = 30.seconds,
            waterType = WaterType.FILTERED,
            sourceSessionId = "session-0",
            rating = 4.5f,
            timesUsed = 3,
            lastUsedAt = EPOCH,
            label = null,
            isActive = true,
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )
    }
}
