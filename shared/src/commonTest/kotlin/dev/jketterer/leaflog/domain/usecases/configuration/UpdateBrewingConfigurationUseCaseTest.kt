package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.FakeBrewingConfigurationRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class UpdateBrewingConfigurationUseCaseTest {
    private val repository = FakeBrewingConfigurationRepository(listOf(method()))
    private val update = UpdateBrewingConfigurationUseCase(repository, Clock.System)

    @Test
    fun `a plan with no increment and no overrides clears both`() = runTest {
        update("m", plan = BrewingPlan(60.seconds)).getOrThrow()

        val saved = repository.getById("m")!!
        assertEquals(60.seconds, saved.brewingTime)
        assertNull(saved.steepIncrement)
        assertEquals(emptyMap(), saved.steepOverrides)
    }

    @Test
    fun `without a plan the existing one is kept`() = runTest {
        update("m", label = "Renamed").getOrThrow()

        assertEquals(BrewingPlan(20.seconds, 5.seconds, mapOf(4 to 45.seconds)), repository.getById("m")!!.plan)
    }

    private fun method() = BrewingConfiguration(
        id = "m",
        teaId = "tea",
        vesselId = "gaiwan",
        teaQuantityGrams = 5f,
        waterQuantityMl = 100.0,
        temperatureCelsius = 95.0,
        brewingTime = 20.seconds,
        steepIncrement = 5.seconds,
        steepOverrides = mapOf(4 to 45.seconds),
        waterType = WaterType.FILTERED,
        sourceSessionId = null,
        rating = null,
        timesUsed = 0,
        lastUsedAt = null,
        label = null,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
    )
}
