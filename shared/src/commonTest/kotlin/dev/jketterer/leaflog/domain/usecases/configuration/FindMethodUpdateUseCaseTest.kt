package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.FakeBrewingConfigurationRepository
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FindMethodUpdateUseCaseTest {
    private val method = BrewingConfiguration(
        id = "method",
        teaId = "tea-1",
        vesselId = "vessel-1",
        teaQuantityGrams = 5f,
        waterQuantityMl = 100.0,
        temperatureCelsius = 95.0,
        brewingTime = 20.seconds,
        steepIncrement = 5.seconds,
        waterType = WaterType.FILTERED,
        sourceSessionId = null,
        rating = null,
        timesUsed = 0,
        lastUsedAt = null,
        label = null,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
    )

    private fun find() =
        FindMethodUpdateUseCase(FakeBrewingConfigurationRepository(listOf(method)))

    // Brewed as the method plans: 20s, 25s, 30s
    private fun brewed(
        times: List<Duration> = listOf(20.seconds, 25.seconds, 30.seconds),
        rating: Float? = null,
        vesselId: String = "vessel-1",
        usedMethod: String? = "method",
        edit: (SessionWithSteeps) -> SessionWithSteeps = { it },
    ): SessionWithSteeps {
        val base = previewSession(
            ratings = times.map { rating },
            temperatureCelsius = 95.0,
            waterQuantityMl = 100.0
        )
        val session = base.copy(
            session = base.session.copy(vesselId = vesselId, usedConfigurationId = usedMethod),
            steeps = base.steeps.mapIndexed { i, steep -> steep.copy(brewingTime = times[i]) },
        )
        return edit(session)
    }

    private suspend fun found(session: SessionWithSteeps) = find()(session)

    @Test
    fun `a brew that followed the method has nothing to update`() = runTest {
        assertNull(found(brewed()))
    }

    @Test
    fun `a steep timed differently offers the method`() = runTest {
        assertEquals(method, found(brewed(times = listOf(20.seconds, 25.seconds, 40.seconds))))
    }

    @Test
    fun `a late tap that rounds to the plan is not a deviation`() = runTest {
        assertNull(found(brewed(times = listOf(21.seconds, 26.seconds, 29.seconds))))
    }

    @Test
    fun `a changed parameter offers the method`() = runTest {
        assertEquals(
            method,
            found(brewed { it.copy(session = it.session.copy(teaQuantityGrams = 7f)) })
        )
        assertEquals(
            method,
            found(brewed { it.copy(session = it.session.copy(waterType = WaterType.SPRING)) })
        )
        assertEquals(
            method,
            found(brewed { s -> s.copy(steeps = s.steeps.map { it.copy(waterQuantityMl = 120.0) }) }),
        )
    }

    @Test
    fun `a rinse that differs from the method's either way offers the method`() = runTest {
        assertEquals(method, found(brewed { it.copy(session = it.session.copy(rinsed = true)) }))

        val rinsing = method.copy(rinse = true)
        val found = FindMethodUpdateUseCase(FakeBrewingConfigurationRepository(listOf(rinsing)))(brewed())
        assertEquals(rinsing, found)
    }

    @Test
    fun `a temperature within a degree is not a deviation`() = runTest {
        assertNull(found(brewed { s -> s.copy(steeps = s.steeps.map { it.copy(temperatureCelsius = 94.5) }) }))
        assertEquals(
            method,
            found(brewed { s -> s.copy(steeps = s.steeps.map { it.copy(temperatureCelsius = 90.0) }) }),
        )
    }

    @Test
    fun `a different vessel is not an update`() = runTest {
        assertNull(found(brewed(vesselId = "other", times = listOf(40.seconds))))
    }

    @Test
    fun `a session under 3 stars is not offered`() = runTest {
        assertNull(found(brewed(times = listOf(40.seconds), rating = 2f)))
    }

    @Test
    fun `a session that used no method has nothing to update`() = runTest {
        assertNull(found(brewed(times = listOf(40.seconds), usedMethod = null)))
    }
}
