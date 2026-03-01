package dev.jketterer.leaflog.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.ZipArchiver
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import dev.jketterer.leaflog.data.local.preferences.PreferencesDataStore
import dev.jketterer.leaflog.domain.services.TimerLifecycleHandler
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Room database
    single {
        val context = get<Context>()
        val dbFile = context.getDatabasePath("leaflog.db")

        Room.databaseBuilder<LeafLogDatabase>(
            context = context,
            name = dbFile.absolutePath,
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
    single { TimerNotificationServiceImpl(context = get()) } bind TimerNotificationService::class

    single {
        TimerLifecycleHandler(context = get())
    }

    // Image storage
    single { ImageStorage(context = get()) }

    // Zip archiver
    single { ZipArchiver() }

    // Preferences
    single {
        PreferencesDataStore(context = get())
    }
}