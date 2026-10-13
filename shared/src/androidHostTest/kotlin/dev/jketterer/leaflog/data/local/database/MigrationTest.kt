package dev.jketterer.leaflog.data.local.database

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
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
}

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
