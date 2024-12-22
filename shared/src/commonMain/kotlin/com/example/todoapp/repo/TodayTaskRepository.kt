package com.example.todoapp.repo

import co.touchlab.kermit.Logger
import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.todotask.HabitTaskWithFewDetails
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.data.todotask.TodayTaskWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.globalViewModels.UserInitManager
import com.kizitonwose.calendar.core.minusDays
import com.kizitonwose.calendar.core.plusDays
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class TodayTaskRepository(private val database: AppDatabase) {
    private val todayTaskDao: TodayTaskDao by lazy {
        database.getTodayTaskDao()
    }


    suspend fun getAllTasks(): Flow<List<TodayTaskWithFewDetails>> {
        return todayTaskDao.getTodayTaskWithDetails()
    }

    suspend fun getAllTasksForDate1(date: MyDate): Flow<List<TodayTaskWithFewDetails>> {
        val x = todayTaskDao.getAllTasksForDate1(date.dateString)
        Logger.e("Returning list1: $x")
        return x
    }

    suspend fun getAllTasksForDate2(date: MyDate): Flow<List<TodayTaskWithFewDetails>> {
        val x = todayTaskDao.getAllTasksForDate2(date.dateString)
        Logger.e("Returning list2: $x")
        return x
    }
    //update status
    suspend fun updateStatus(todayTaskId: Long, status: String) {
        todayTaskDao.updateTaskStatusById(todayTaskId, status)
    }
    //update tag
    suspend fun updateTag(todayTaskId: Long, tag: String) {
        todayTaskDao.updateTaskTagById(todayTaskId, tag)
    }

    suspend fun insertFullTask(task: TodayTaskWithFewDetails): Long {
        return todayTaskDao.insertFullTask(task)
    }

    suspend fun getHabitMissions(date: MyDate): Flow<List<HabitTaskWithFewDetails>> {
        return todayTaskDao.getHabitMissions(date.dateString)
    }


    suspend fun getTaskProgressForPastAround(missionId: Long, date: LocalDate): Flow<List<TodayTask>> {
        val from_date = MyDate.fromLocalDate(date.minusDays(8)).dateString
        val to_date = MyDate.fromLocalDate(date.plusDays(2)).dateString
        return todayTaskDao.getTaskProgressForPastAround(missionId,from_date, to_date)
    }


    suspend fun addRandomMissionForDay(currentDate: LocalDate): Long {
        return todayTaskDao.addRandomMission(currentDate)
    }

}