package dev.jketterer.leaflog.di

import androidx.room.Room
import androidx.room.RoomDatabase
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import platform.Foundation.NSHomeDirectory

actual fun getDatabaseBuilder(): RoomDatabase.Builder<LeafLogDatabase> {
    val dbFilePath = NSHomeDirectory() + "/leaflog.db"
    return Room.databaseBuilder<LeafLogDatabase>(
        name = dbFilePath,
    )
}