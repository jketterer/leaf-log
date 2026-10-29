package dev.jketterer.leaflog.data.repositories

import dev.jketterer.leaflog.data.local.database.at
import dev.jketterer.leaflog.data.local.database.entities.SteepTagEntity
import dev.jketterer.leaflog.data.local.database.inMemoryDatabase
import dev.jketterer.leaflog.data.local.database.row
import dev.jketterer.leaflog.data.local.database.seedForeignKeys
import dev.jketterer.leaflog.domain.models.SessionStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock

class TastingTagRepositoryImplTest {

    private val database = inMemoryDatabase()
    private val repository = TastingTagRepositoryImpl(
        database.tastingTagDao(),
        object : Clock {
            override fun now() = at(5)
        },
    )

    @BeforeTest
    fun seed() = runTest {
        database.seedForeignKeys()
        database.teaSessionDao().upsertAll(
            listOf("both", "carried", "confirmed", "neither", "deleted").mapIndexed { i, id ->
                row(id, null, 1, SessionStatus.COMPLETED, "vessel", at(i))
            },
        )
        database.teaSessionDao().softDelete("deleted", timestamp = at(0).toEpochMilliseconds())
        repository.initializeDefaults()
    }

    @AfterTest
    fun close() = database.close()

    @Test
    fun `merging a tag confirms its target on every steep where either was confirmed`() = runTest {
        val peachy = repository.createCustom("Peachy")
        tag(
            SteepTagEntity("both", peachy.id, confirmed = false),
            SteepTagEntity("both", "fruity", confirmed = true),
            SteepTagEntity("carried", peachy.id, confirmed = false),
            SteepTagEntity("confirmed", peachy.id, confirmed = true),
            SteepTagEntity("confirmed", "fruity", confirmed = false),
        )

        repository.copyToSteeps(fromId = peachy.id, toId = "fruity")
        repository.delete(peachy.id)

        assertEquals(
            setOf(
                SteepTagEntity("both", "fruity", confirmed = true),
                SteepTagEntity("carried", "fruity", confirmed = false),
                SteepTagEntity("confirmed", "fruity", confirmed = true),
            ),
            database.teaSessionDao().getAllTags().toSet(),
        )
    }

    @Test
    fun `deleting a tag takes it off its steeps and stamps them and the tag`() = runTest {
        val peachy = repository.createCustom("Peachy")
        tag(SteepTagEntity("both", peachy.id, confirmed = true), SteepTagEntity("both", "fruity", confirmed = true))

        repository.delete(peachy.id)

        assertEquals(listOf(SteepTagEntity("both", "fruity", confirmed = true)), database.teaSessionDao().getAllTags())
        assertEquals(at(5), database.teaSessionDao().getById("both")?.updatedAt)
        assertEquals(at(3), database.teaSessionDao().getById("neither")?.updatedAt)
        val deleted = database.tastingTagDao().getAllIncludingDeleted().single { it.id == peachy.id }
        assertNotNull(deleted.deletedAt)
        assertEquals(at(5), deleted.updatedAt)
    }

    @Test
    fun `renaming a tag trims the name and stamps the tag`() = runTest {
        val peachy = repository.createCustom("Peahcy")

        repository.rename(peachy.id, " Peachy ")

        val renamed = database.tastingTagDao().getAllIncludingDeleted().single { it.id == peachy.id }
        assertEquals("Peachy", renamed.name)
        assertEquals(at(5), renamed.updatedAt)
    }

    private suspend fun tag(vararg tags: SteepTagEntity) = database.teaSessionDao().insertTags(tags.toList())
}
