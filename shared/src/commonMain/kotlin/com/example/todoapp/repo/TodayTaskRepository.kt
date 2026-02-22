package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.db.models.TaskType
import com.example.todoapp.screen.diary.ComboTask
import com.kizitonwose.calendar.core.minusDays
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusDays
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class TodayTaskRepository(private val database: AppDatabase) {
    private val todayTaskDao: TodayTaskDao by lazy {
        database.getTodayTaskDao()
    }

    suspend fun getAllTasksForMission(missionId: Long): Flow<List<TodayTaskWithFewDetails>> {
        return todayTaskDao.getAllTasksForMission(missionId)
    }

    suspend fun getAllTasks(): Flow<List<TodayTaskWithFewDetails>> {
        return todayTaskDao.getTodayTaskWithDetails()
    }

    suspend fun getAllTasksForDate1(date: MyDate): Flow<List<ComboTask>> {
        return todayTaskDao.getAllTasksForDate1(date.dateString)
    }

    suspend fun getTasksPastMonth(): Flow<List<TodayTask>> {
        val sinceDate = MyDate.fromLocalDate(LocalDate.now().minusDays(60)).dateString
        return todayTaskDao.getTasksPastMonth(sinceDate)
    }

    suspend fun getAllTasksForDate2(date: MyDate): Flow<List<TodayTaskWithFewDetails>> {
        return todayTaskDao.getAllTasksForDate2(date.dateString)
    }

    suspend fun updateStatus(todayTaskId: Long, status: String) {
        todayTaskDao.updateTaskStatusById(todayTaskId, status)
    }

    suspend fun updateTag(todayTaskId: Long, tag: String) {
        todayTaskDao.updateTaskTagById(todayTaskId, tag)
    }

    suspend fun insertFullTask(task: TodayTaskWithFewDetails): Long {
        return todayTaskDao.insertFullTask(task)
    }

    suspend fun updateTodayTask(todayTask: TodayTask) {
         todayTaskDao.updateTodayTask(todayTask)
    }

    suspend fun getTaskProgressForPastAround(missionId: Long, date: LocalDate): Flow<List<TodayTask>?> {
        val fromDate = MyDate.fromLocalDate(date.minusDays(8)).dateString
        val toDate = MyDate.fromLocalDate(date.plusDays(2)).dateString
        return todayTaskDao.getTaskProgressForPastAround(missionId, fromDate, toDate)
    }

    suspend fun addMissionForDay(date: LocalDate, mission: Mission): Long {
        if (mission.missionId > 0) {
            val newTask = prepareTaskObject(mission.missionId, date)
            return todayTaskDao.insertFullTask(newTask)
        }
        return -1L
    }

    suspend fun deleteTaskForToday(taskId: Long) {
        return todayTaskDao.deleteTaskForToday(taskId)
    }

    suspend fun activateTaskForToday(taskId: Long) {
        return todayTaskDao.activateTaskForToday(taskId)
    }

    suspend fun addRandomMissionForDay(currentDate: LocalDate): Long {
        val newMissionId = todayTaskDao.getNewMissionForDay(MyDate.fromLocalDate(currentDate).dateString)
        if (newMissionId > 0) {
            val newTask = prepareTaskObject(newMissionId, currentDate)
            return todayTaskDao.insertFullTask(newTask)
        }
        return -1L
    }

    private fun prepareTaskObject(missionId: Long, date: LocalDate): TodayTaskWithFewDetails {
        return TodayTaskWithFewDetails(
            todayTask = TodayTask(
                todayTaskId = 0L,
                userId = 1L,
                missionId = missionId,
                taskDate = MyDate.fromLocalDate(date),
                taskStatus = TaskStatus.ADDED.value,
                taskType = TaskType.AG_RANDOM.value,
                taskPageTag = TaskType.TODO.value,
                taskProgressVal = 0f,
                taskText = "This is a randomly picked task",
                taskPictureUrl = null,
                active = "Y",
                taskLink = null
            ),
            todayTaskReminder = TodayTaskReminder(
                todayTaskId = 0L,
                timeOfDay = "08:00",
                alarmTone = "Default Tone",
                timeBefore = 10,
                timeBeforeUnit = 1
            ),
            mission = null,
            pillar = null,
            missionFrequency = null
        )
    }
}
