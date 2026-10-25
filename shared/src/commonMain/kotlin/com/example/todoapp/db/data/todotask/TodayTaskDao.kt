package com.example.todoapp.db.data.todotask

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodayTaskDao {

    @Insert
    suspend fun insert(task: TodayTask): Long

    @Insert
    suspend fun insertAll(tasks: List<TodayTask>)

    @Update
    suspend fun update(task: TodayTask)

    @Query("DELETE FROM today_task WHERE today_task_id = :taskId")
    suspend fun delete(taskId: Long)

    /** A mission's still-planned items from [from] on. Dates are yyyy-MM-dd, so text order is date order. */
    @Query("DELETE FROM today_task WHERE task_mission_id = :missionId AND task_date >= :from AND task_state = 'PLANNED'")
    suspend fun deletePlannedFrom(missionId: Long, from: String)

    @Query("SELECT * FROM today_task WHERE today_task_id = :taskId")
    suspend fun get(taskId: Long): TodayTask?

    @Query("SELECT * FROM today_task WHERE task_date BETWEEN :from AND :to ORDER BY task_date, task_order")
    suspend fun getBetween(from: String, to: String): List<TodayTask>

    @Query("SELECT * FROM today_task WHERE task_date BETWEEN :from AND :to ORDER BY task_date, task_order")
    fun observeBetween(from: String, to: String): Flow<List<TodayTask>>
}
