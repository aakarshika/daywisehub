package com.example.todoapp.db.data.mood

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.todoapp.db.data.todotask.SubTask
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface TodayMoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMood(mood: Mood): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodayMood(todayMood: TodayMood): Long

    @Query("""
        SELECT * 
        FROM mood m 
        left join today_mood tm on tm.tm_mood_id = m.mood_id and tm.tm_date = :selectedDate
        """)
    fun getTodayMood(selectedDate: String): Flow<List<TodayMoodWithDetails>>

    @Query("""
        SELECT * 
        FROM mood m 
        
        """)
    fun getAllMoods(): Flow<List<TodayMoodWithDetails>>

}