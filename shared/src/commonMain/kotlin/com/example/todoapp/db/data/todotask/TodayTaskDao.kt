package com.example.todoapp.db.data.todotask

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    /** A move: the copy goes in on its new day and the item is marked moved, both or neither. */
    @Transaction
    suspend fun move(copy: TodayTask, moved: TodayTask) {
        insert(copy)
        update(moved)
    }

    /** Taking a move back: the moved item is done on its own day and its planned copy goes, both or neither. */
    @Transaction
    suspend fun takeBack(done: TodayTask, copyId: Long) {
        update(done)
        delete(copyId)
    }

    /** A swap: the item [oldId] goes and [task] comes in its place, both or neither. */
    @Transaction
    suspend fun replace(oldId: Long, task: TodayTask) {
        delete(oldId)
        insert(task)
    }

    /** A mission's still-planned items from [from] on. Dates are yyyy-MM-dd, so text order is date order. */
    @Query("DELETE FROM today_task WHERE task_mission_id = :missionId AND task_date >= :from AND task_state = 'PLANNED'")
    suspend fun deletePlannedFrom(missionId: Long, from: String)

    @Query("SELECT * FROM today_task WHERE today_task_id = :taskId")
    suspend fun get(taskId: Long): TodayTask?

    /** The earliest day with any item, or null on an empty install. */
    @Query("SELECT MIN(task_date) FROM today_task")
    suspend fun getFirstDate(): String?

    /** [getFirstDate], as it changes: an item added on an earlier day moves it back. */
    @Query("SELECT MIN(task_date) FROM today_task")
    fun observeFirstDate(): Flow<String?>

    /** Items still planned on a day before [date], newest day first. */
    @Query("SELECT * FROM today_task WHERE task_date < :date AND task_state = 'PLANNED' ORDER BY task_date DESC, task_order")
    suspend fun getPlannedBefore(date: String): List<TodayTask>

    @Query("SELECT * FROM today_task WHERE task_date BETWEEN :from AND :to ORDER BY task_date, task_order")
    suspend fun getBetween(from: String, to: String): List<TodayTask>

    @Query("SELECT * FROM today_task WHERE task_date BETWEEN :from AND :to ORDER BY task_date, task_order")
    fun observeBetween(from: String, to: String): Flow<List<TodayTask>>

    /** Rewrites a day's rows unchanged, so this process's queries on them run again (the iPhone widget writes from another process). */
    @Query("UPDATE today_task SET task_state = task_state WHERE task_date = :date")
    suspend fun touchDay(date: String)
}
