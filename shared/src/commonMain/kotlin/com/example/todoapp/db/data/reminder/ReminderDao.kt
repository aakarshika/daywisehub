package com.example.todoapp.db.data.reminder

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminder_settings WHERE reminder_id = 1")
    fun observe(): Flow<ReminderSettings?>

    @Upsert
    suspend fun upsert(settings: ReminderSettings)
}
