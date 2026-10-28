package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class GetBrewingParametersPrefillUseCaseTest {

    // --- a default temperature ------------------------------------------------------------------

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

    // --- a past brew of the pair ----------------------------------------------------------------

    @Test
    fun `the pair's last brew fills the form and is its source`() = runTest {
        val brew = session(temperatureCelsius = 88.0, 4.5f)

        val prefill = prefillFor(tea = tea(defaultTemperatureCelsius = 82.0), sessions = listOf(brew))

        assertEquals(88.0, prefill.temperatureCelsius)
        assertEquals(5f, prefill.teaQuantityGrams)
        assertEquals(PrefillSource.PastBrew(brew), prefill.source)
    }

    @Test
    fun `a past brew fills steep 1 at its plan's rounded time`() = runTest {
        val brew = session(temperatureCelsius = 88.0, 4.5f).let {
            it.copy(steeps = listOf(it.steeps.single().copy(actualTime = 47.seconds)))
        }

        val prefill = prefillFor(tea = tea(defaultTemperatureCelsius = null), sessions = listOf(brew))

        assertEquals(45.seconds, prefill.brewingTime)
    }

    @Test
    fun `a brew in another vessel is no source`() = runTest {
        val elsewhere = session(88.0, 5f).let { it.copy(session = it.session.copy(vesselId = "mug")) }

        val prefill = prefillFor(tea = tea(defaultTemperatureCelsius = 82.0), sessions = listOf(elsewhere))

        assertEquals(PrefillSource.TeaDefault, prefill.source)
    }

    @Test
    fun `a session still brewing is never a source`() = runTest {
        val prefill = prefillFor(
            tea = tea(defaultTemperatureCelsius = 82.0),
            sessions = listOf(session(88.0, 5f, status = SessionStatus.IN_PROGRESS)),
        )

        assertEquals(PrefillSource.TeaDefault, prefill.source)
    }

    // --- helpers --------------------------------------------------------------------------------

    private suspend fun prefillFor(
        tea: Tea,
        teaType: TeaType = teaType(defaultTemperatureCelsius = null),
        sessions: List<SessionWithSteeps> = emptyList(),
    ): BrewingParametersPrefill {
        val useCase = GetBrewingParametersPrefillUseCase(
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
