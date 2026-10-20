package dev.jketterer.leaflog.domain.usecases.configuration

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.session.FakeBrewingConfigurationRepository
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ShouldOfferSaveAsMethodUseCaseTest {
    private fun offer(vararg saved: BrewingConfiguration) =
        FakeBrewingConfigurationRepository(saved.toList()).let {
            ShouldOfferSaveAsMethodUseCase(it, CheckDuplicateConfigurationUseCase(it))
        }

    @Test
    fun `an unrated session is offered`() = runTest {
        assertTrue(offer()(previewSession(ratings = listOf(null))))
    }

    @Test
    fun `a multi-steep session is offered`() = runTest {
        assertTrue(offer()(previewSession(ratings = listOf(4f, 4.5f, 5f))))
    }

    @Test
    fun `a session rated under 3 stars is not offered`() = runTest {
        assertFalse(offer()(previewSession(ratings = listOf(2f, 3f))))
    }

    @Test
    fun `a session that used a method on the same vessel is not offered`() = runTest {
        val method = matchingMethod().copy(id = "method", brewingTime = 3.minutes)

        assertFalse(offer(method)(previewSession(ratings = listOf(5f)).usingMethod("method")))
    }

    @Test
    fun `a session that used a method on another vessel is offered`() = runTest {
        val method = matchingMethod().copy(id = "method", vesselId = "other", brewingTime = 3.minutes)

        assertTrue(offer(method)(previewSession(ratings = listOf(5f)).usingMethod("method")))
    }

    @Test
    fun `a session matching a saved method is not offered`() = runTest {
        assertFalse(offer(matchingMethod())(previewSession(ratings = listOf(5f))))
    }

    private fun SessionWithSteeps.usingMethod(id: String) =
        copy(session = session.copy(usedConfigurationId = id))

    // previewSession: 5 g, 200 ml, 80 °C, 2 min, one steep
    private fun matchingMethod() = BrewingConfiguration(
        id = "m",
        teaId = "tea-1",
        vesselId = "vessel-1",
        teaQuantityGrams = 5f,
        waterQuantityMl = 200.0,
        temperatureCelsius = 80.0,
        brewingTime = 2.minutes,
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
