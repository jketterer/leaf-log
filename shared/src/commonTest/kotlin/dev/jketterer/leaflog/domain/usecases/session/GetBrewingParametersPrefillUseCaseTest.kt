package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
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
            sessions = listOf(session(temperatureCelsius = 88.0, 4.5f)),
        )

        assertEquals(88.0, prefill.temperatureCelsius)
        assertEquals(PrefillSource.BestRatedSession(4.5f), prefill.source)
    }

    @Test
    fun `a past brew is suggested at the time it actually brewed`() = runTest {
        val brew = session(temperatureCelsius = 88.0, 4.5f).let {
            it.copy(steeps = listOf(it.steeps.single().copy(actualTime = 47.seconds)))
        }

        val prefill = prefillFor(tea = tea(defaultTemperatureCelsius = null), sessions = listOf(brew))

        assertEquals(47.seconds, prefill.brewingTime)
    }

    // A brew you did not enjoy is skipped, and the default is the next thing down.
    @Test
    fun aPoorlyRatedBrewFallsThroughToTheTeaDefault() = runTest {
        val tea = tea(defaultTemperatureCelsius = 82.0)
        val prefill = prefillFor(
            tea = tea,
            sessions = listOf(session(temperatureCelsius = 88.0, 1.0f)),
        )

        assertEquals(82.0, prefill.temperatureCelsius)
        assertEquals(PrefillSource.TeaDefault, prefill.source)
    }

    @Test
    fun `a gongfu session is suggested by its steeps' mean rating`() = runTest {
        val prefill = prefillFor(
            tea = tea(defaultTemperatureCelsius = null),
            sessions = listOf(session(88.0, 2f, null, 5f)),
        )

        assertEquals(PrefillSource.BestRatedSession(3.5f), prefill.source)
    }

    @Test
    fun `a session still brewing is never suggested`() = runTest {
        val prefill = prefillFor(
            tea = tea(defaultTemperatureCelsius = 82.0),
            sessions = listOf(session(88.0, 5f, status = SessionStatus.IN_PROGRESS)),
        )

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
        sessions: List<SessionWithSteeps> = emptyList(),
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

    // previewSession brews tea-1 in vessel-1: the tea and VESSEL here
    private fun session(
        temperatureCelsius: Double,
        vararg ratings: Float?,
        status: SessionStatus = SessionStatus.COMPLETED,
    ) = previewSession(
        ratings = ratings.toList(),
        status = status,
        temperatureCelsius = temperatureCelsius,
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
