package com.example.todoapp.db.data.todotask

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.todoapp.screen.diary.ComboTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TodayTaskDao {

    //insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodayTask(todayTask: TodayTask): Long

    @Update
    suspend fun updateTodayTask(todayTask: TodayTask)

    //insert today task reminder
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodayTaskReminder(todayTaskReminder: TodayTaskReminder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTask): Long


    @Query("""SELECT tt.*,tr.*,m.*,p.*,mf.* 
        FROM today_task  tt
        LEFT JOIN today_task_reminder tr ON tt.today_task_id = tr.rem_task_id
        LEFT JOIN mission m ON tt.task_mission_id = m.mission_id
        LEFT JOIN mission_pillar_mapping mp ON m.mission_id = mp.mission_mapping_id 
        LEFT JOIN pillar p ON mp.pillar_mapping_id = p.pillar_id
        LEFT JOIN mission_frequency mf ON tt.task_mission_id = mf.fs_mission_id
        WHERE m.mission_id = :missionId
        """)
    fun getAllTasksForMission(missionId: Long): Flow<List<TodayTaskWithFewDetails>>


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
        where tt.task_date = :date and tt.task_active <> 'N'
        """)
    fun getAllTasksForDate1(date: String): Flow<List<ComboTask>>

    @Query("""SELECT tt.*
        FROM today_task tt
        WHERE tt.task_date >= :sinceDate
        """)
    fun getTasksPastMonth(sinceDate: String): Flow<List<TodayTask>>


    @Query("""SELECT tt.*
        FROM today_task  tt 
        JOIN mission m ON tt.task_mission_id = m.mission_id
        where m.mission_id = :missionId and tt.task_date between :dateString and :dateString1
        """)
    fun getTaskProgressForPastAround(
        missionId: Long,
        dateString: String, dateString1: String): Flow<List<TodayTask>>


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



    @Query("""
        SELECT m.mission_id
        FROM mission m
        LEFT JOIN mission_frequency mf ON m.mission_id = mf.fs_mission_id
        WHERE mf.is_daily_habit = false
        AND m.mission_id NOT IN (
                SELECT DISTINCT task_mission_id
                FROM today_task
                WHERE task_date = :currentDate )
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun getNewMissionForDay(currentDate: String): Long

    @Query("""
        update  today_task
        set task_active = 'Y'
        WHERE today_task_id = :todayTaskId
    """)
    suspend fun activateTaskForToday(todayTaskId: Long)



    @Query("""
        update  today_task
        set task_active = 'N'
        WHERE today_task_id = :todayTaskId
    """)
    suspend fun deleteTaskForToday(todayTaskId: Long)


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
            updateTodayTask(task.todayTask)
            upsertTodayTaskReminder(task.todayTaskReminder!!)
            return task.todayTask.todayTaskId
        } else {
            val todayTaskId = insertTodayTask(task.todayTask)
            val rem = task.todayTaskReminder!!.copy(
                todayTaskId = todayTaskId
            )
            upsertTodayTaskReminder(rem)
            return todayTaskId
        }
    }

}