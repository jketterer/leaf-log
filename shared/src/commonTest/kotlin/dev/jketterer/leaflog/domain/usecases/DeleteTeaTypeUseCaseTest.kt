package dev.jketterer.leaflog.domain.usecases

import dev.jketterer.leaflog.domain.models.DomainFailure
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.usecases.session.FakeTeaRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTeaTypeRepository
import dev.jketterer.leaflog.domain.usecases.session.FakeTransactor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class DeleteTeaTypeUseCaseTest {

    private val teas = FakeTeaRepository(listOf(TEA))
    private val types = FakeTeaTypeRepository(listOf(type("green"), type("oolong")))

    private val deleteTeaType = DeleteTeaTypeUseCase(FakeTransactor(), teas, types)

    @Test
    fun `a type no tea uses is deleted`() = runTest {
        deleteTeaType("oolong").getOrThrow()

        assertNull(types.getById("oolong"))
    }

    @Test
    fun `a type a tea uses is refused with the count`() = runTest {
        val result = deleteTeaType("green")

        val failure = assertIs<DomainFailure.InUse>(result.exceptionOrNull())
        assertEquals(1, failure.count)
        assertEquals(DomainFailure.Dependents.TEAS, failure.by)
        assertNotNull(types.getById("green"))
    }

    @Test
    fun `the last type is refused`() = runTest {
        deleteTeaType("oolong").getOrThrow()
        teas.delete(TEA.id)

        val result = deleteTeaType("green")

        assertIs<DomainFailure.LastRemaining>(result.exceptionOrNull())
        assertNotNull(types.getById("green"))
    }

    private companion object {
        val EPOCH = Instant.fromEpochMilliseconds(0)

        val TEA = Tea(
            id = "tea",
            name = "Dragon Well",
            teaTypeId = "green",
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
