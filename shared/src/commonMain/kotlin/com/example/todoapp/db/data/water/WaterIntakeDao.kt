package com.example.todoapp.db.data.water

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterIntakeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWaterIntake(waterIntake: WaterIntake): Long

    @Query("""
        SELECT * 
        FROM water_intake w 
        where w.water_date = :selectedDate
        limit 1
        """)
    fun getWaterIntake(selectedDate: String): Flow<WaterIntake?>

    @Query("SELECT * FROM water_intake WHERE water_date BETWEEN :from AND :to")
    fun observeBetween(from: String, to: String): Flow<List<WaterIntake>>


    /** As TodayTaskDao.touchDay. */
    @Query("UPDATE water_intake SET notes = notes WHERE water_date = :date")
    suspend fun touchDay(date: String)
}
