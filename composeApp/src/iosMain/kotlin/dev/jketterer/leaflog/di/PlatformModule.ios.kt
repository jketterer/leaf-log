package dev.jketterer.leaflog.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.data.local.ZipArchiver
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import dev.jketterer.leaflog.data.local.database.MIGRATION_1_2
import dev.jketterer.leaflog.data.local.preferences.PreferencesDataStore
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.services.TimerLifecycleHandler
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual fun platformModule() = module {
    single {
        val dbFilePath = NSHomeDirectory() + "/Documents/leaflog.db"
        Room.databaseBuilder<LeafLogDatabase>(
            name = dbFilePath,
        )
            .addMigrations(MIGRATION_1_2)
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
    // TimerNotificationServiceImpl extends NSObject (ObjC), so it cannot also implement a Kotlin
    // interface. We wrap it in an anonymous object so Koin only indexes the Kotlin interface KClass.
    single<TimerNotificationService> {
        val impl = TimerNotificationServiceImpl()
        object : TimerNotificationService {
            override fun showTimerRunning(state: TimerState) = impl.showTimerRunning(state)
            override fun showTimerComplete(teaName: String, sessionId: String?) = impl.showTimerComplete(teaName, sessionId)
            override fun scheduleCompletionAlarm(teaName: String, remainingSeconds: Double, sessionId: String?) = impl.scheduleCompletionAlarm(teaName, remainingSeconds, sessionId)
            override fun cancelCompletionAlarm() = impl.cancelCompletionAlarm()
            override fun onTimerStopped() = impl.onTimerStopped()
        }
    }

    single {
        TimerLifecycleHandler()
    }

    // Image storage
    single { ImageStorage() }

    // Zip archiver
    single { ZipArchiver() }

    // Preferences
    single {
        PreferencesDataStore()
    }
}