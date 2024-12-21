package com.example.todoapp.db.data.todotask

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow

@Dao
interface TodayTaskDao {

    //insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodayTask(todayTask: TodayTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTodayTask(todayTask: TodayTask)

    //insert today task reminder
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodayTaskReminder(todayTaskReminder: TodayTaskReminder): Long

    //insert subtask
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTask): Long

    //getalltasks
    @Query("SELECT * FROM today_task WHERE task_user_id = :userId")
    fun getAllTodayTasks(userId: Long): Flow<List<TodayTask>>

    //get reminder for task id
    @Query("SELECT * FROM today_task_reminder WHERE rem_task_id = :todayTaskId")
    fun getTodayTaskReminder(todayTaskId: Long): Flow<TodayTaskReminder>

    //get task with details
    @Query("""SELECT tt.*,tr.*,m.*,p.*,mf.* 
        FROM today_task  tt
        LEFT JOIN today_task_reminder tr ON tt.today_task_id = tr.rem_task_id
        LEFT JOIN mission m ON tt.task_mission_id = m.mission_id
        LEFT JOIN mission_pillar_mapping mp ON m.mission_id = mp.mission_mapping_id 
        LEFT JOIN pillar p ON mp.pillar_mapping_id = p.pillar_id
        LEFT JOIN mission_frequency mf ON tt.task_mission_id = mf.fs_mission_id
        """)
    fun getTodayTaskWithDetails(): Flow<List<TodayTaskWithFewDetails>>


    @Query("""SELECT tt.*,tr.*,m.*,p.*,mf.* 
        FROM today_task  tt
        LEFT JOIN today_task_reminder tr ON tt.today_task_id = tr.rem_task_id
        LEFT JOIN mission m ON tt.task_mission_id = m.mission_id
        LEFT JOIN mission_pillar_mapping mp ON m.mission_id = mp.mission_mapping_id 
        LEFT JOIN pillar p ON mp.pillar_mapping_id = p.pillar_id
        LEFT JOIN mission_frequency mf ON tt.task_mission_id = mf.fs_mission_id
        where tt.task_date = :date
        """)
    fun getAllTasksForDate1(date: String): Flow<List<TodayTaskWithFewDetails>>



    @Query("""SELECT tt.*,tr.*,m.*,p.*,mf.* 
        FROM today_task  tt
        LEFT JOIN today_task_reminder tr ON tt.today_task_id = tr.rem_task_id
        LEFT JOIN mission m ON tt.task_mission_id = m.mission_id
        LEFT JOIN mission_pillar_mapping mp ON m.mission_id = mp.mission_mapping_id 
        LEFT JOIN pillar p ON mp.pillar_mapping_id = p.pillar_id
        LEFT JOIN mission_frequency mf ON tt.task_mission_id = mf.fs_mission_id
        where tt.task_date = :date
        """)
    fun getAllTasksForDate2(date: String): Flow<List<TodayTaskWithFewDetails>>


    //getdetails for tssk id
    @Query("""SELECT tt.*,tr.*,m.*,p.*,mf.* 
        FROM today_task  tt
        LEFT JOIN today_task_reminder tr ON tt.today_task_id = tr.rem_task_id
        LEFT JOIN mission m ON tt.task_mission_id = m.mission_id
        LEFT JOIN mission_pillar_mapping mp ON m.mission_id = mp.mission_mapping_id 
        LEFT JOIN pillar p ON mp.pillar_mapping_id = p.pillar_id
        LEFT JOIN mission_frequency mf ON tt.task_mission_id = mf.fs_mission_id
        WHERE tt.today_task_id = :todayTaskId
        """)
    fun getTodayTaskWithDetails(todayTaskId: Long): Flow<TodayTaskWithFewDetails>

    //gey subtasks for task id
    @Query("""SELECT * FROM sub_task  
        WHERE sub_today_task_id = :todayTaskId
        """)
    fun getSubTasks(todayTaskId: Long): Flow<List<SubTask>>

    //update sub task list with task id
    @Transaction
    suspend fun insertSubTasks(subTasks: List<SubTask>, todayTaskId: Long) {
        subTasks.forEach { subTask ->
            insertSubTask(subTask.copy(todayTaskId = todayTaskId))
        }
    }



    //update task status by id
    @Query("""
        UPDATE today_task
        SET task_status = :status
        WHERE today_task_id = :todayTaskId
    """)
    suspend fun updateTaskStatusById(todayTaskId: Long, status: String)

    //update task tag by id
    @Query("""
        UPDATE today_task
        SET task_page_tag = :tag
        WHERE today_task_id = :todayTaskId
    """)
    suspend fun updateTaskTagById(todayTaskId: Long, tag: String)

    @Transaction
    suspend fun insertFullTask(task: TodayTaskWithFewDetails): Long {

        if (task.todayTask.todayTaskId > 0L) {
            Logger.e( "updating task ${task.todayTask}")
            updateTodayTask(task.todayTask)
            Logger.e( "updating task reminder ${task.todayTaskReminder}")
            upsertTodayTaskReminder(task.todayTaskReminder!!)
            return task.todayTask.todayTaskId
        } else {
            Logger.e( "Inserting task ${task.todayTask}")
            val todayTaskId = insertTodayTask(task.todayTask)
            val rem = task.todayTaskReminder!!.copy(
                todayTaskId = todayTaskId
            )
            Logger.e( "Inserting task reminder ${task.todayTaskReminder}")
            upsertTodayTaskReminder(rem)
            return todayTaskId
        }
    }
}