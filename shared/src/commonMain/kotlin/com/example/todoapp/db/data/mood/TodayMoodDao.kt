package com.example.todoapp.db.data.mood

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodayMoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMood(mood: Mood): Long

    @Query("SELECT COUNT(*) FROM mood")
    suspend fun getMoodCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodayMood(todayMood: TodayMood): Long

    @Query("""
        SELECT * 
        FROM mood m 
        left join today_mood tm on tm.tm_mood_id = m.mood_id and tm.tm_date = :selectedDate
        """)
    fun getTodayMood(selectedDate: String): Flow<List<TodayMoodWithDetails>>

    /** The moods picked on each day from [from] to [to], by icon name. */
    @Query("""
        SELECT m.mood_icon AS icon, tm.tm_date AS date
        FROM today_mood tm
        JOIN mood m ON m.mood_id = tm.tm_mood_id
        WHERE tm.tm_mood_status = 'ACTIVE' AND tm.tm_date BETWEEN :from AND :to
        """)
    fun observeActiveBetween(from: String, to: String): Flow<List<DayMood>>

    @Query("""
        SELECT * 
        FROM mood m 
        
        """)
    fun getAllMoods(): Flow<List<TodayMoodWithDetails>>


    /** As TodayTaskDao.touchDay. */
    @Query("UPDATE today_mood SET tm_mood_status = tm_mood_status WHERE tm_date = :date")
    suspend fun touchDay(date: String)
}