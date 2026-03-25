package de.familienwecker.famwake.db

import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSHomeDirectory

fun getDatabaseBuilder(): RoomDatabase.Builder<FamWakeDatabase> {
    val dbFile = NSHomeDirectory() + "/famwake.db"
    return Room.databaseBuilder<FamWakeDatabase>(
        name = dbFile
    ).setDriver(BundledSQLiteDriver())
}
