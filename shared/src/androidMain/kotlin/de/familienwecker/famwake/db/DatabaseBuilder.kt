package de.familienwecker.famwake.db

import android.content.Context
import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<FamWakeDatabase> {
    val dbFile = context.getDatabasePath("famwake.db")
    return Room.databaseBuilder<FamWakeDatabase>(
        context = context,
        name = dbFile.absolutePath
    ).setDriver(BundledSQLiteDriver()) // Empfohlen für KMP
}
