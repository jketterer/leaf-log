package dev.jketterer.leaflog.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun getDatabaseBuilder(): RoomDatabase.Builder<LeafLogDatabase> {
    val appContext = DatabaseHelper.getContext()
    val dbFile = appContext.getDatabasePath("leaflog.db")
    return Room.databaseBuilder<LeafLogDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}

private object DatabaseHelper : KoinComponent {
    fun getContext(): Context = get()
}