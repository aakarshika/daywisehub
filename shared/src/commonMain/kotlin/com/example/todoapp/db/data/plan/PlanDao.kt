package com.example.todoapp.db.data.plan

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM day_plan WHERE plan_date = :date")
    suspend fun getPlan(date: String): DayPlan?

    @Query("SELECT * FROM day_plan WHERE plan_date = :date")
    fun observePlan(date: String): Flow<DayPlan?>

    @Upsert
    suspend fun upsertPlan(plan: DayPlan)

    @Query("SELECT * FROM day_settings ORDER BY settings_weekday")
    suspend fun getSettings(): List<DaySettings>

    @Query("SELECT * FROM day_settings ORDER BY settings_weekday")
    fun observeSettings(): Flow<List<DaySettings>>

    @Upsert
    suspend fun upsertSettings(settings: List<DaySettings>)
}
