package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.usecases.session.FakeTeaRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTransactor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class MergeTeaTypeUseCaseTest {

    private val teas = FakeTeaRepository(listOf(TEA))
    private val types = FakeTeaTypeRepository(listOf(type("green"), type("oolong")))

    private val mergeTeaType = MergeTeaTypeUseCase(FakeTransactor(), teas, types)

    @Test
    fun `a merged type's teas move to the other type and it is deleted`() = runTest {
        mergeTeaType(sourceId = "green", targetId = "oolong").getOrThrow()

        assertEquals("oolong", teas.getById(TEA.id)!!.teaTypeId)
        assertEquals(TEA.defaultTemperatureCelsius, teas.getById(TEA.id)!!.defaultTemperatureCelsius)
        assertNull(types.getById("green"))
    }

    @Test
    fun `a merge into a deleted type is refused and nothing moves`() = runTest {
        types.delete("oolong")

        val result = mergeTeaType(sourceId = "green", targetId = "oolong")

        assertTrue(result.isFailure)
        assertEquals("green", teas.getById(TEA.id)!!.teaTypeId)
        assertNotNull(types.getById("green"))
    }

    private companion object {
        val EPOCH = Instant.fromEpochMilliseconds(0)

        val TEA = Tea(
            id = "tea",
            name = "Dragon Well",
            teaTypeId = "green",
            defaultTemperatureCelsius = 80.0,
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )

        fun type(id: String) = TeaType(
            id = id,
            name = id,
            colorHex = "#4CAF50",
            isSystemDefault = true,
            displayOrder = 0,
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )
    }
}
