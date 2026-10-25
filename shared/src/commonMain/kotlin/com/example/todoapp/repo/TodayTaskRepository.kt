package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class TodayTaskRepository(private val database: AppDatabase) {

    private val todayTaskDao: TodayTaskDao by lazy { database.getTodayTaskDao() }

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TodayTask>> =
        todayTaskDao.observeBetween(key(from), key(to))

    suspend fun getBetween(from: LocalDate, to: LocalDate): List<TodayTask> =
        todayTaskDao.getBetween(key(from), key(to))

    suspend fun get(taskId: Long): TodayTask? = todayTaskDao.get(taskId)

    suspend fun insert(task: TodayTask): Long = todayTaskDao.insert(task)

    suspend fun insertAll(tasks: List<TodayTask>) = todayTaskDao.insertAll(tasks)

    suspend fun update(task: TodayTask) = todayTaskDao.update(task)

    suspend fun delete(taskId: Long) = todayTaskDao.delete(taskId)

    private fun key(date: LocalDate): String = MyDate.fromLocalDate(date).dateString
}
