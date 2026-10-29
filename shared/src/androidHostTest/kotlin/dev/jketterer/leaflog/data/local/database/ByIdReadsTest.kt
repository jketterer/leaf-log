package dev.jketterer.leaflog.data.local.database

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ByIdReadsTest {

    private val database = inMemoryDatabase()

    @BeforeTest
    fun seed() = runTest {
        database.seedForeignKeys()
    }

    @AfterTest
    fun close() = database.close()

    @Test
    fun `a soft-deleted tea is not found by its id`() = runTest {
        val dao = database.teaDao()
        assertNotNull(dao.getById("tea"))

        dao.softDelete("tea", timestamp = 1)

        assertNull(dao.getById("tea"))
        assertNull(dao.getByIdFlow("tea").first())
    }

    @Test
    fun `a soft-deleted vessel is not found by its id`() = runTest {
        val dao = database.brewingVesselDao()
        assertNotNull(dao.getById("vessel"))

        dao.softDelete("vessel", timestamp = 1)

        assertNull(dao.getById("vessel"))
        assertNull(dao.getByIdFlow("vessel").first())
    }

    @Test
    fun `a soft-deleted tea type is not found by its id`() = runTest {
        val dao = database.teaTypeDao()
        assertNotNull(dao.getById("type"))

        dao.softDelete("type", timestamp = 1)

        assertNull(dao.getById("type"))
        assertNull(dao.getByIdFlow("type").first())
    }
}
