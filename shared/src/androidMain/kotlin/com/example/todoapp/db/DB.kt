package com.example.todoapp.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("my_room.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
        // Real data is kept from version 5 on: 5 → 6 is an auto migration (Database.kt), and a missing
        // migration from 5 or later stops with an error instead of wiping. Databases from before the
        // redesign (1 to 4) are wiped, and so is one from a newer build when an older build is installed.
        .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4)
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
}

fun getDatabase(ctx: Context): AppDatabase {
    return getDatabaseBuilder(ctx).build()
}
