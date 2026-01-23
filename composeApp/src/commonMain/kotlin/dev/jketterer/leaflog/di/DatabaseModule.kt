package dev.jketterer.leaflog.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module

expect fun getDatabaseBuilder(): RoomDatabase.Builder<LeafLogDatabase>

val databaseModule = module {
    single {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    single { get<LeafLogDatabase>().teaDao() }
    single { get<LeafLogDatabase>().teaSessionDao() }
    single { get<LeafLogDatabase>().teaTypeDao() }
    single { get<LeafLogDatabase>().brewingVesselDao() }
}