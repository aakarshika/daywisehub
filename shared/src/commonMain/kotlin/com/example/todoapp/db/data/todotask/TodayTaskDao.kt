package com.example.todoapp.db.data.todotask

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.diary.ComboTask
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

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
        where tt.task_date = :date and tt.task_active = 'Y'
        """)
    fun getAllTasksForDate1(date: String): Flow<List<ComboTask>>

    @Query("""SELECT tt.*
        FROM today_task  tt
        """)
    fun getTasksPastMonth(): Flow<List<TodayTask>>


    @Query("""SELECT tt.*,tr.*,m.*,p.*,mf.* 
        FROM mission m
        LEFT JOIN today_task  tt ON tt.task_mission_id = m.mission_id and tt.task_date = :date
        LEFT JOIN today_task_reminder tr ON tt.today_task_id = tr.rem_task_id
        LEFT JOIN mission_pillar_mapping mp ON m.mission_id = mp.mission_mapping_id 
        LEFT JOIN pillar p ON mp.pillar_mapping_id = p.pillar_id
        LEFT JOIN mission_frequency mf ON m.mission_id = mf.fs_mission_id
        where mf.is_daily_habit = true 
        """)
    fun getHabitMissions(date: String): Flow<List<ComboTask>>

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
        where tt.task_date = :date and mf.is_daily_habit != true
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
        Logger.e("insertFullTask  $task")
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

    suspend fun addMissionForDay(date: LocalDate, mission: Mission): Long{
        if(mission.missionId>0) {
            val newTask = prepareTaskObject(mission.missionId, date)
            Logger.e("newTask prepared from ${mission.missionId}: $newTask")
            val tid = insertFullTask(newTask)
            Logger.e("inserted taskid: $tid")
            return tid
        }
        return -1L
    }
    suspend fun addRandomMission(currentDate: LocalDate): Long{
        Logger.e("adding RandomMission for date $currentDate")
        val newMissionId = getNewMissionForDay(MyDate.fromLocalDate(currentDate).dateString)
        Logger.e("adding RandomMission newMissionId: $newMissionId")
        if(newMissionId>0) {
            val newTask = prepareTaskObject(newMissionId, currentDate)
            Logger.e("newTask prepared: ${newTask.todayTask}")
            val tid = insertFullTask(newTask)
            Logger.e("inserted taskid: $tid")
            return tid
        }
        return -1L
    }

    private fun prepareTaskObject(randomMissionId: Long, currentDate: LocalDate): TodayTaskWithFewDetails {

        val newTask = TodayTaskWithFewDetails(
            todayTask = TodayTask(
                todayTaskId = 0L, // Example task ID
                userId = 1L, // Example userId
                missionId = randomMissionId, // Example missionId
                taskDate = MyDate.fromLocalDate(currentDate), // currentDate, // Example task date (current date)
                taskStatus = "ADDED", // Example status
                taskType = "AG_RANDOM", // Example type
                taskPageTag = "TODO",
                taskProgressVal = 0f, // Example progress
                taskText = "This is a randomly picked task", // Example text
                taskPictureUrl = null, // Example URL (can be null)
                taskLink = null // Example link (can be null)
            ),
            todayTaskReminder = TodayTaskReminder(
                todayTaskId = 0L, // Example task ID
                timeOfDay = "08:00", // Example time
                alarmTone = "Default Tone", // Example alarm tone
                timeBefore = 10, // Example time before
                timeBeforeUnit = 1 // Example time before unit (e.g., minutes)
            ),
            mission = null,
            pillar = null,
            missionFrequency = null
        )
        return newTask
    }

}