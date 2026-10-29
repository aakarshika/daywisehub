package com.example.todoapp.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSUserDomainMask


/** Shared with the widget extension; the app and the extension both carry this group in their entitlements. */
const val APP_GROUP = "group.com.ideeya.inbalance"

private const val DB_NAME = "my_room.db"

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = databasePath()
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
        factory =  { AppDatabase::class.instantiateImpl() }
    )
        // Real data is kept from version 5 on: 5 → 6 and 6 → 7 are auto migrations (Database.kt), and a missing
        // migration from 5 or later stops with an error instead of wiping. Databases from before the
        // redesign (1 to 4) are wiped, and so is one from a newer build when an older build is installed.
        .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4)
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
}

fun getDatabase(): AppDatabase {
    return getDatabaseBuilder().build()
}


/** The database lives in the App Group container, so the widgets read and write the same one. Without the group (an unsigned build), it's in Documents. */
@OptIn(ExperimentalForeignApi::class)
private fun databasePath(): String {
    val group = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(APP_GROUP)?.path
    return (group ?: documentDirectory()) + "/" + DB_NAME
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}