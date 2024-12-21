package com.example.todoapp.repo

import co.touchlab.kermit.Logger
import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.todotask.TodayTaskWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.globalViewModels.UserInitManager
import kotlinx.coroutines.flow.Flow

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

}