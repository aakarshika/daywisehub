package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.reminder.ReminderDao
import com.example.todoapp.db.data.reminder.ReminderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepository(private val database: AppDatabase) {

    private val reminderDao: ReminderDao by lazy { database.getReminderDao() }

    fun observe(): Flow<ReminderSettings> = reminderDao.observe().map { it ?: ReminderSettings() }

    suspend fun save(settings: ReminderSettings) = reminderDao.upsert(settings.copy(id = 1))
}
