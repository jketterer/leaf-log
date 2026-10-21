package dev.jketterer.leaflog.di

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
import dev.jketterer.leaflog.data.local.preferences.PreferencesDataStore
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.services.TimerLifecycleHandler
import dev.jketterer.leaflog.domain.services.TimerNotificationService
import dev.jketterer.leaflog.domain.services.TimerNotificationServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.FileSystem
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual fun platformModule() = module {
    single {
        val dbFilePath = NSHomeDirectory() + "/Documents/leaflog.db"
        Room.databaseBuilder<LeafLogDatabase>(
            name = dbFilePath,
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
    // TimerNotificationServiceImpl extends NSObject (ObjC), so it cannot also implement a Kotlin
    // interface. We wrap it in an anonymous object so Koin only indexes the Kotlin interface KClass.
    single<TimerNotificationService> {
        val impl = TimerNotificationServiceImpl()
        object : TimerNotificationService {
            override fun showTimerRunning(state: TimerState) = impl.showTimerRunning(state)
            override fun showTimerPaused(state: TimerState) = impl.showTimerPaused(state)
            override fun showTimerComplete(state: TimerState) = impl.showTimerComplete()
            override fun cancelTimerComplete() = impl.cancelTimerComplete()
            override fun scheduleCompletionAlarm(state: TimerState) = impl.scheduleCompletionAlarm(state)
            override fun cancelCompletionAlarm() = impl.cancelCompletionAlarm()
            override fun scheduleSessionReminder(teaName: String, sessionId: String, delaySeconds: Double) = impl.scheduleSessionReminder(teaName, sessionId, delaySeconds)
            override fun cancelSessionReminder() = impl.cancelSessionReminder()
            override fun onTimerStopped() = impl.onTimerStopped()
            override suspend fun hasNotificationPermission() = impl.hasNotificationPermission()
        }
    }

    single {
        TimerLifecycleHandler()
    }

    // Image storage
    single { ImageStorage() }
    single { FileSystem.SYSTEM }

    // Preferences
    single {
        PreferencesDataStore()
    }
}