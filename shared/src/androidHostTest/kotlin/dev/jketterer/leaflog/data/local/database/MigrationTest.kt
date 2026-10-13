package dev.jketterer.leaflog.data.local.database

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MigrationTest {

    @Test
    fun `5 to 6 produces the exported version 6 schema and keeps sessions`() {
        val connection = createFromSchema(5)
        connection.execSQL(
            """
            INSERT INTO tea_session (id, teaId, steepNumber, status, vesselId, waterType,
                timestamp, brewingTime, temperatureCelsius, waterQuantityMl, photos, syncStatus,
                createdAt, updatedAt)
            VALUES ('s1', 't1', 1, 'COMPLETED', 'v1', 'FILTERED', 0, 30000, 95.0, 100.0, '[]',
                'LOCAL', 0, 0)
            """
        )

        MIGRATION_5_6.migrate(connection)

        assertSchemaMatches(connection, 6)
        connection.prepare("SELECT id, timerTotalMs FROM tea_session").use {
            assertTrue(it.step())
            assertEquals("s1", it.getText(0))
            assertTrue(it.isNull(1))
        }
        connection.close()
    }

    @Test
    fun `6 to 7 produces the exported version 7 schema and moves session fields to the parent`() {
        val connection = createFromSchema(6)
        connection.execSQL(
            "INSERT INTO tea (id, name, teaTypeId, photos, isFavorite, totalSessions, " +
                "createdAt, updatedAt, syncStatus) " +
                "VALUES ('t1', 'Tea', 'type', '[]', 0, 0, 0, 0, 'LOCAL')",
        )
        // Left mid-steep on steep 1
        connection.insertV6Steep("one", null, 1, "IN_PROGRESS")
        // Version 6 completed the parent when steep 2 started
        connection.insertV6Steep("gongfu", null, 1, "COMPLETED")
        connection.insertV6Steep("gongfu-2", "gongfu", 2, "COMPLETED")
        connection.insertV6Steep("gongfu-3", "gongfu", 3, "IN_PROGRESS")
        connection.insertV6Steep("done", null, 1, "COMPLETED")
        connection.insertV6Steep("done-2", "done", 2, "COMPLETED")
        // A deleted steep doesn't keep its session open
        connection.insertV6Steep("gone", null, 1, "COMPLETED")
        connection.insertV6Steep("gone-2", "gone", 2, "IN_PROGRESS", deletedAt = 1)

        MIGRATION_6_7.migrate(connection)

        assertSchemaMatches(connection, 7)
        val rows = connection.prepare(
            "SELECT id, status, vesselId, waterType, teaQuantityGrams, location, " +
                "usedConfigurationId, actualBrewingTimeMs FROM tea_session ORDER BY id",
        ).use { statement ->
            buildMap {
                while (statement.step()) {
                    put(
                        statement.getText(0),
                        List(7) { statement.textOrNull(it + 1) },
                    )
                }
            }
        }
        val sessionFields = listOf("v1", "FILTERED", "5.0", "desk", "c1")

        assertEquals(
            mapOf(
                "done" to listOf("COMPLETED") + sessionFields + null,
                "done-2" to listOf("COMPLETED", null, null, null, null, null, null),
                "gone" to listOf("COMPLETED") + sessionFields + null,
                "gone-2" to listOf("IN_PROGRESS", null, null, null, null, null, null),
                "gongfu" to listOf("IN_PROGRESS") + sessionFields + null,
                "gongfu-2" to listOf("COMPLETED", null, null, null, null, null, null),
                "gongfu-3" to listOf("IN_PROGRESS", null, null, null, null, null, null),
                "one" to listOf("IN_PROGRESS") + sessionFields + null,
            ),
            rows,
        )
        connection.prepare("SELECT totalSessions FROM tea").use {
            assertTrue(it.step())
            assertEquals(2, it.getLong(0))
        }
        connection.close()
    }
}

private fun SQLiteStatement.textOrNull(index: Int) = if (isNull(index)) null else getText(index)

private fun SQLiteConnection.insertV6Steep(
    id: String,
    parentId: String?,
    steepNumber: Int,
    status: String,
    deletedAt: Long? = null,
) = execSQL(
    """
    INSERT INTO tea_session (id, teaId, parentSessionId, steepNumber, status, teaQuantityGrams,
        vesselId, waterType, location, usedConfigurationId, averageRating, timestamp,
        brewingTime, temperatureCelsius, waterQuantityMl, photos, syncStatus, createdAt,
        updatedAt, deletedAt)
    VALUES ('$id', 't1', ${parentId?.let { "'$it'" }}, $steepNumber, '$status', 5.0, 'v1',
        'FILTERED', 'desk', 'c1', 4.0, $steepNumber, 30000, 95.0, 100.0, '[]', 'LOCAL', 0, 0,
        $deletedAt)
    """,
)

private val schemaDir = File("schemas/${LeafLogDatabase::class.qualifiedName}")

private fun schema(version: Int): JsonObject =
    Json.parseToJsonElement(File(schemaDir, "$version.json").readText())
        .jsonObject.getValue("database").jsonObject

private val JsonObject.entities
    get() = getValue("entities").jsonArray.map { it.jsonObject }

private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

/** An in-memory database built from an exported schema, as Room would have created it. */
private fun createFromSchema(version: Int): SQLiteConnection {
    val connection = BundledSQLiteDriver().open(":memory:")
    schema(version).entities.forEach { entity ->
        val table = entity.string("tableName")
        val statements = listOf(entity.string("createSql")) +
            entity["indices"]?.jsonArray.orEmpty().map { it.jsonObject.string("createSql") }
        statements.forEach { connection.execSQL(it.replace("\${TABLE_NAME}", table)) }
    }
    return connection
}

/** Compares every table the way Room validates on open: columns, foreign keys and indices. */
private fun assertSchemaMatches(migrated: SQLiteConnection, version: Int) {
    createFromSchema(version).use { expected ->
        schema(version).entities.map { it.string("tableName") }.forEach { table ->
            assertEquals(
                TableInfo.read(expected, table).asEntity(),
                TableInfo.read(migrated, table),
                "Table $table differs from $version.json",
            )
        }
    }
}

/**
 * Room checks a column's default only when the entity declares one, so `ADD COLUMN ... DEFAULT
 * NULL` passes on device. Marking the expected side as entity-sourced applies the same leniency.
 */
private fun TableInfo.asEntity() = TableInfo(
    name,
    columns.mapValues { (_, c) ->
        TableInfo.Column(
            c.name, c.type, c.notNull, c.primaryKeyPosition, c.defaultValue,
            TableInfo.CREATED_FROM_ENTITY,
        )
    },
    foreignKeys,
    indices,
)
