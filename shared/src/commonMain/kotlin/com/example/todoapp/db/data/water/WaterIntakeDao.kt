package com.example.todoapp.db.data.mood

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

}