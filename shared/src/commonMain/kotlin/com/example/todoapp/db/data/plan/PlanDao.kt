package com.example.todoapp.db.data.plan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM day_plan WHERE plan_date = :date")
    suspend fun getPlan(date: String): DayPlan?

    @Query("SELECT * FROM day_plan WHERE plan_date = :date")
    fun observePlan(date: String): Flow<DayPlan?>

    @Query("SELECT * FROM day_plan WHERE plan_date BETWEEN :from AND :to ORDER BY plan_date")
    fun observePlansBetween(from: String, to: String): Flow<List<DayPlan>>

    @Upsert
    suspend fun upsertPlan(plan: DayPlan)

    @Query("DELETE FROM today_task WHERE today_task_id IN (:taskIds)")
    suspend fun deleteTasks(taskIds: List<Long>)

    @Insert
    suspend fun insertTasks(tasks: List<TodayTask>)

    /** A day's picks: [removed] go, [picks] come in and [plan] is saved, all or none. */
    @Transaction
    suspend fun savePicks(removed: List<Long>, picks: List<TodayTask>, plan: DayPlan) {
        if (removed.isNotEmpty()) deleteTasks(removed)
        insertTasks(picks)
        upsertPlan(plan)
    }

    /** Days before [date] that have a plan but whose fixed blocks haven't been ticked yet. */
    @Query("SELECT * FROM day_plan WHERE plan_date < :date AND plan_fixed_ticked = 0 ORDER BY plan_date")
    suspend fun getUntickedBefore(date: String): List<DayPlan>

    @Query("SELECT * FROM day_settings ORDER BY settings_weekday")
    suspend fun getSettings(): List<DaySettings>

    @Query("SELECT * FROM day_settings ORDER BY settings_weekday")
    fun observeSettings(): Flow<List<DaySettings>>

    @Upsert
    suspend fun upsertSettings(settings: List<DaySettings>)
}
