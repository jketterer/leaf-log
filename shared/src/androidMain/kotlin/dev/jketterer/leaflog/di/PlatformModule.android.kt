package dev.jketterer.leaflog.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import dev.jketterer.leaflog.data.local.database.MIGRATION_1_2
import dev.jketterer.leaflog.data.local.database.MIGRATION_2_3
import dev.jketterer.leaflog.data.local.database.MIGRATION_3_4
import dev.jketterer.leaflog.data.local.database.MIGRATION_4_5
import dev.jketterer.leaflog.data.local.database.MIGRATION_5_6
import dev.jketterer.leaflog.data.local.database.MIGRATION_6_7
import dev.jketterer.leaflog.data.local.database.MIGRATION_7_8
import dev.jketterer.leaflog.data.local.database.MIGRATION_8_9
import dev.jketterer.leaflog.data.local.database.MIGRATION_9_10
import dev.jketterer.leaflog.data.local.database.MIGRATION_10_11
import dev.jketterer.leaflog.data.local.database.MIGRATION_11_12
import dev.jketterer.leaflog.data.local.preferences.PreferencesDataStore
import dev.jketterer.leaflog.domain.services.TimerLifecycleHandler
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
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
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
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
    single { get<LeafLogDatabase>().tastingTagDao() }

    // Platform-specific services
    single { TimerNotificationServiceImpl(context = get()) } bind TimerNotificationService::class

    single {
        TimerLifecycleHandler(context = get())
    }

    // Image storage
    single { ImageStorage(context = get()) }
    single { FileSystem.SYSTEM }

    // Preferences
    single {
        PreferencesDataStore(context = get())
    }
}