package dev.jketterer.leaflog.domain.usecases.vessel

import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.usecases.session.FakeBrewingVesselRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTransactor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class SetVesselArchivedUseCaseTest {

    private val now = Instant.fromEpochMilliseconds(1_000)
    private val vessels = FakeBrewingVesselRepository(listOf(vessel("gaiwan"), vessel("mug")))

    private val setArchived = SetVesselArchivedUseCase(
        FakeTransactor(),
        vessels,
        object : Clock {
            override fun now() = now
        },
    )

    @Test
    fun `an archived vessel is stamped with when`() = runTest {
        setArchived("gaiwan", archived = true).getOrThrow()

        val gaiwan = vessels.getById("gaiwan")!!
        assertTrue(gaiwan.isArchived)
        assertEquals(now, gaiwan.updatedAt)
    }

    @Test
    fun `an archived vessel is brought back`() = runTest {
        setArchived("gaiwan", archived = true).getOrThrow()

        setArchived("gaiwan", archived = false).getOrThrow()

        assertFalse(vessels.getById("gaiwan")!!.isArchived)
    }

    @Test
    fun `the last active vessel is refused and kept active`() = runTest {
        setArchived("gaiwan", archived = true).getOrThrow()

        val result = setArchived("mug", archived = true)

        assertIs<DomainFailure.LastRemaining>(result.exceptionOrNull())
        assertFalse(vessels.getById("mug")!!.isArchived)
    }

    @Test
    fun `archiving a vessel already archived changes nothing`() = runTest {
        setArchived("gaiwan", archived = true).getOrThrow()

        setArchived("gaiwan", archived = true).getOrThrow()

        assertTrue(vessels.getById("gaiwan")!!.isArchived)
    }

    @Test
    fun `a vessel that doesn't exist is not found`() = runTest {
        assertIs<DomainFailure.NotFound>(setArchived("kettle", archived = true).exceptionOrNull())
    }

    private companion object {
        val EPOCH = Instant.fromEpochMilliseconds(0)

        fun vessel(id: String) = BrewingVessel(
            id = id,
            name = id,
            iconName = null,
            isSystemDefault = false,
            displayOrder = 0,
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )
    }
}
