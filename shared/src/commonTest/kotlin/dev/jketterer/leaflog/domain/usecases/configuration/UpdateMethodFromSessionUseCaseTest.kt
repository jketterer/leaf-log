package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.FakeBrewingConfigurationRepository
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class UpdateMethodFromSessionUseCaseTest {
    // A 20s plan growing 5s a steep, pinned to 60s at steep 8
    private val method = BrewingConfiguration(
        id = "method",
        teaId = "tea-1",
        vesselId = "vessel-1",
        teaQuantityGrams = 5f,
        waterQuantityMl = 100.0,
        temperatureCelsius = 95.0,
        brewingTime = 20.seconds,
        steepIncrement = 5.seconds,
        steepOverrides = mapOf(3 to 40.seconds, 8 to 60.seconds),
        waterType = WaterType.FILTERED,
        sourceSessionId = null,
        rating = null,
        timesUsed = 0,
        lastUsedAt = null,
        label = "Mine",
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
    )

    // Four steeps of 10s, 20s, 30s, 40s, brewed hotter, with more leaf, in a different water
    private val session = previewSession(
        ratings = listOf(null, null, null, null),
        temperatureCelsius = 90.0,
        waterQuantityMl = 120.0,
        teaQuantityGrams = 7f,
    ).let { s ->
        s.copy(
            session = s.session.copy(waterType = WaterType.SPRING),
            steeps = s.steeps.mapIndexed { i, steep -> steep.copy(brewingTime = (10 * (i + 1)).seconds) },
        )
    }

    @Test
    fun `the brew's set-up and plan replace the method's, keeping overrides past the last steep`() = runTest {
        val repository = FakeBrewingConfigurationRepository(listOf(method))

        val updated = UpdateMethodFromSessionUseCase(repository)("method", session).getOrThrow()

        assertEquals(10.seconds, updated.brewingTime)
        assertEquals(10.seconds, updated.steepIncrement)
        assertEquals(mapOf(8 to 60.seconds), updated.steepOverrides)
        assertEquals(7f, updated.teaQuantityGrams)
        assertEquals(120.0, updated.waterQuantityMl)
        assertEquals(90.0, updated.temperatureCelsius)
        assertEquals(WaterType.SPRING, updated.waterType)
        assertEquals("Mine", updated.label)
        assertEquals("vessel-1", updated.vesselId)
        assertEquals(updated, repository.getById("method"))
    }

    @Test
    fun `a method that is gone fails`() = runTest {
        val result = UpdateMethodFromSessionUseCase(FakeBrewingConfigurationRepository())("method", session)

        assertEquals(true, result.isFailure)
    }
}
