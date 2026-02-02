package dev.jketterer.leaflog.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import dev.jketterer.leaflog.data.local.preferences.PreferencesDataStore
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual fun platformModule() = module {
    single {
        val dbFilePath = NSHomeDirectory() + "/leaflog.db"
        Room.databaseBuilder<LeafLogDatabase>(
            name = dbFilePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    // DAOs
    single { get<LeafLogDatabase>().teaDao() }
    single { get<LeafLogDatabase>().teaSessionDao() }
    single { get<LeafLogDatabase>().teaTypeDao() }
    single { get<LeafLogDatabase>().brewingVesselDao() }
    single { get<LeafLogDatabase>().brewingConfigurationDao() }

    // Platform-specific services
    single {
        TimerNotificationService()
    }

    // Preferences
    single {
        PreferencesDataStore()
    }
}