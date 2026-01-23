package dev.jketterer.leaflog.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.jketterer.leaflog.data.local.database.dao.BrewingVesselDao
import dev.jketterer.leaflog.data.local.database.dao.TeaDao
import dev.jketterer.leaflog.data.local.database.dao.TeaSessionDao
import dev.jketterer.leaflog.data.local.database.dao.TeaTypeDao
import dev.jketterer.leaflog.data.local.database.entities.BrewingVesselEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaTypeEntity

@Database(
    entities = [
        TeaEntity::class,
        TeaSessionEntity::class,
        TeaTypeEntity::class,
        BrewingVesselEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LeafLogDatabase : RoomDatabase() {
    abstract fun teaDao(): TeaDao
    abstract fun teaTypeDao(): TeaTypeDao
    abstract fun teaSessionDao(): TeaSessionDao
    abstract fun brewingVesselDao(): BrewingVesselDao
}