package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.todotask.TodayTaskWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.screen.globalViewModels.UserInitManager
import kotlinx.coroutines.flow.Flow

class TodayTaskRepository(private val database: AppDatabase) {
    private val todayTaskDao: TodayTaskDao by lazy {
        database.getTodayTaskDao()
    }


    suspend fun getAllTasks(): Flow<List<TodayTaskWithFewDetails>> {
        return todayTaskDao.getTodayTaskWithDetails()
    }
    //update status
    suspend fun updateStatus(todayTaskId: Long, status: String) {
        todayTaskDao.updateTaskStatusById(todayTaskId, status)
    }
    //update tag
    suspend fun updateTag(todayTaskId: Long, tag: String) {
        todayTaskDao.updateTaskTagById(todayTaskId, tag)
    }

}