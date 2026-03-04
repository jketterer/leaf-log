package dev.jketterer.leaflog.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import dev.jketterer.leaflog.data.local.database.dao.BrewingConfigurationDao
import dev.jketterer.leaflog.data.local.database.dao.BrewingVesselDao
import dev.jketterer.leaflog.data.local.database.dao.TeaDao
import dev.jketterer.leaflog.data.local.database.dao.TeaSessionDao
import dev.jketterer.leaflog.data.local.database.dao.TeaTypeDao
import dev.jketterer.leaflog.data.local.database.entities.BrewingConfigurationEntity
import dev.jketterer.leaflog.data.local.database.entities.BrewingVesselEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaTypeEntity

// Room KSP generates the actual implementations for each platform.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object LeafLogDatabaseConstructor : RoomDatabaseConstructor<LeafLogDatabase>

@Database(
    entities = [
        TeaEntity::class,
        TeaSessionEntity::class,
        TeaTypeEntity::class,
        BrewingVesselEntity::class,
        BrewingConfigurationEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(LeafLogDatabaseConstructor::class)
@TypeConverters(Converters::class)
abstract class LeafLogDatabase : RoomDatabase() {
    abstract fun teaDao(): TeaDao
    abstract fun teaTypeDao(): TeaTypeDao
    abstract fun teaSessionDao(): TeaSessionDao
    abstract fun brewingVesselDao(): BrewingVesselDao
    abstract fun brewingConfigurationDao(): BrewingConfigurationDao
}