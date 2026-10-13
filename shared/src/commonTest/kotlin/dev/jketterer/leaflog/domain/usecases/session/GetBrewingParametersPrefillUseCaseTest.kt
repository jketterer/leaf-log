package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class GetBrewingParametersPrefillUseCaseTest {

    // --- the new steps ------------------------------------------------------------------------

    @Test
    fun theTeasOwnDefaultIsUsedWhenThereIsNoHistory() = runTest {
        val prefill = prefillFor(tea = tea(defaultTemperatureCelsius = 82.0))

        assertEquals(82.0, prefill.temperatureCelsius)
        assertEquals(PrefillSource.TeaDefault, prefill.source)
    }

    @Test
    fun theTypeDefaultIsUsedWhenTheTeaHasNoneOfItsOwn() = runTest {
        val prefill = prefillFor(
            tea = tea(defaultTemperatureCelsius = null),
            teaType = teaType(defaultTemperatureCelsius = 75.0),
        )

        assertEquals(75.0, prefill.temperatureCelsius)
        assertEquals(PrefillSource.TypeDefault("Green"), prefill.source)
    }

    @Test
    fun theTeasOwnDefaultBeatsItsTypes() = runTest {
        val prefill = prefillFor(
            tea = tea(defaultTemperatureCelsius = 82.0),
            teaType = teaType(defaultTemperatureCelsius = 75.0),
        )

        assertEquals(82.0, prefill.temperatureCelsius)
        assertEquals(PrefillSource.TeaDefault, prefill.source)
    }

    @Test
    fun neitherDefaultMeansNoPrefill() = runTest {
        val prefill = prefillFor(
            tea = tea(defaultTemperatureCelsius = null),
            teaType = teaType(defaultTemperatureCelsius = null),
        )

        assertNull(prefill.temperatureCelsius)
        assertEquals(PrefillSource.None, prefill.source)
    }

    @Test
    fun aDefaultTemperatureFillsNothingButTheTemperature() = runTest {
        val prefill = prefillFor(tea = tea(defaultTemperatureCelsius = 82.0))

        assertNull(prefill.teaQuantityGrams)
        assertNull(prefill.waterQuantityMl)
        assertNull(prefill.brewingTime)
        assertNull(prefill.waterType)
    }

    // --- ordering: the new steps must stay below the existing three -----------------------------

    @Test
    fun aSavedConfigBeatsTheTeaDefault() = runTest {
        val tea = tea(defaultTemperatureCelsius = 82.0)
        val prefill = prefillFor(
            tea = tea,
            configurations = listOf(configuration(teaId = tea.id, temperatureCelsius = 93.0)),
        )

        assertEquals(93.0, prefill.temperatureCelsius)
        assertEquals(PrefillSource.SavedConfig, prefill.source)
    }

    @Test
    fun aWellRatedPastBrewBeatsTheTeaDefault() = runTest {
        val tea = tea(defaultTemperatureCelsius = 82.0)
        val prefill = prefillFor(
            tea = tea,
            sessions = listOf(session(teaId = tea.id, temperatureCelsius = 88.0, rating = 4.5f)),
        )

        assertEquals(88.0, prefill.temperatureCelsius)
        assertEquals(PrefillSource.BestRatedSession(4.5f), prefill.source)
    }

    // A brew you did not enjoy is skipped, and the default is the next thing down.
    @Test
    fun aPoorlyRatedBrewFallsThroughToTheTeaDefault() = runTest {
        val tea = tea(defaultTemperatureCelsius = 82.0)
        val prefill = prefillFor(
            tea = tea,
            sessions = listOf(session(teaId = tea.id, temperatureCelsius = 88.0, rating = 1.0f)),
        )

        assertEquals(82.0, prefill.temperatureCelsius)
        assertEquals(PrefillSource.TeaDefault, prefill.source)
    }

    // --- completeness: the guard that keeps tea-bag mode honest ---------------------------------

    @Test
    fun onlyTheMethodSourcesCountAsACompleteMethod() {
        assertTrue(PrefillSource.SavedConfig.isCompleteMethod)
        assertTrue(PrefillSource.BestRatedSession(4f).isCompleteMethod)
        assertTrue(PrefillSource.SameTypeConfig("Longjing").isCompleteMethod)

        // These are the ones that would otherwise be mistaken for a bag-brewed method.
        assertFalse(PrefillSource.TeaDefault.isCompleteMethod)
        assertFalse(PrefillSource.TypeDefault("Green").isCompleteMethod)
        assertFalse(PrefillSource.None.isCompleteMethod)
    }

    // --- helpers --------------------------------------------------------------------------------

    private suspend fun prefillFor(
        tea: Tea,
        teaType: TeaType = teaType(defaultTemperatureCelsius = null),
        configurations: List<BrewingConfiguration> = emptyList(),
        sessions: List<TeaSession> = emptyList(),
    ): BrewingParametersPrefill {
        val useCase = GetBrewingParametersPrefillUseCase(
            brewingConfigurationRepository = FakeBrewingConfigurationRepository(configurations),
            teaRepository = FakeTeaRepository(listOf(tea)),
            teaTypeRepository = FakeTeaTypeRepository(listOf(teaType)),
            teaSessionRepository = FakeTeaSessionRepository(sessions),
        )
        return useCase(tea, VESSEL)
    }

    private fun tea(defaultTemperatureCelsius: Double?) = Tea(
        id = "tea-1",
        name = "Dragon Well",
        teaTypeId = "green",
        defaultTemperatureCelsius = defaultTemperatureCelsius,
        createdAt = EPOCH,
        updatedAt = EPOCH,
    )

    private fun teaType(defaultTemperatureCelsius: Double?) = TeaType(
        id = "green",
        name = "Green",
        defaultTemperatureCelsius = defaultTemperatureCelsius,
        colorHex = "#4CAF50",
        isSystemDefault = true,
        displayOrder = 0,
        createdAt = EPOCH,
        updatedAt = EPOCH,
    )

    private fun configuration(teaId: String, temperatureCelsius: Double) = BrewingConfiguration(
        id = "config-1",
        teaId = teaId,
        vesselId = VESSEL.id,
        teaQuantityGrams = 5f,
        waterQuantityMl = 150.0,
        temperatureCelsius = temperatureCelsius,
        brewingTime = 30.seconds,
        waterType = WaterType.FILTERED,
        sourceSessionId = null,
        rating = null,
        timesUsed = 0,
        lastUsedAt = null,
        label = null,
        isActive = true,
        createdAt = EPOCH,
        updatedAt = EPOCH,
    )

    private fun session(teaId: String, temperatureCelsius: Double, rating: Float) = TeaSession(
        id = "session-1",
        teaId = teaId,
        status = SessionStatus.COMPLETED,
        teaQuantityGrams = 5f,
        vesselId = VESSEL.id,
        waterType = WaterType.FILTERED,
        rating = rating,
        timestamp = EPOCH,
        brewingTime = 30.seconds,
        temperatureCelsius = temperatureCelsius,
        waterQuantityMl = 150.0,
        syncStatus = SyncStatus.LOCAL_ONLY,
        createdAt = EPOCH,
        updatedAt = EPOCH,
    )

    private companion object {
        val EPOCH = Instant.fromEpochMilliseconds(0)

        val VESSEL = BrewingVessel(
            id = "vessel-1",
            name = "Gaiwan",
            iconName = null,
            isSystemDefault = true,
            displayOrder = 0,
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )
    }
}
