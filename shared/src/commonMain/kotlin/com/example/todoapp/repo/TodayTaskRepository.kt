package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class TodayTaskRepository(private val database: AppDatabase) {

    private val todayTaskDao: TodayTaskDao by lazy { database.getTodayTaskDao() }

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TodayTask>> =
        todayTaskDao.observeBetween(key(from), key(to))

    suspend fun getBetween(from: LocalDate, to: LocalDate): List<TodayTask> =
        todayTaskDao.getBetween(key(from), key(to))

    suspend fun get(taskId: Long): TodayTask? = todayTaskDao.get(taskId)

    suspend fun getPlannedBefore(date: LocalDate): List<TodayTask> = todayTaskDao.getPlannedBefore(key(date))

    suspend fun firstDate(): LocalDate? = todayTaskDao.getFirstDate()?.let { MyDate(it).toLocalDate() }

    /** The earliest day with any item, as it changes; null on an empty install. Day badges start there. */
    fun observeFirstDate(): Flow<LocalDate?> = todayTaskDao.observeFirstDate().distinctUntilChanged().map { it?.let { day -> MyDate(day).toLocalDate() } }

    suspend fun insert(task: TodayTask): Long = todayTaskDao.insert(task)

    suspend fun insertAll(tasks: List<TodayTask>) = todayTaskDao.insertAll(tasks)

    suspend fun update(task: TodayTask) = todayTaskDao.update(task)

    suspend fun delete(taskId: Long) = todayTaskDao.delete(taskId)

    /** One transaction: [copy] goes in and [moved] is saved. */
    suspend fun move(copy: TodayTask, moved: TodayTask) = todayTaskDao.move(copy, moved)

    /** One transaction: [done] is saved and the copy [copyId] goes. */
    suspend fun takeBack(done: TodayTask, copyId: Long) = todayTaskDao.takeBack(done, copyId)

    /** One transaction: the item [oldId] goes and [task] goes in. */
    suspend fun replace(oldId: Long, task: TodayTask) = todayTaskDao.replace(oldId, task)

    suspend fun deletePlannedFrom(missionId: Long, from: LocalDate) = todayTaskDao.deletePlannedFrom(missionId, key(from))

    private fun key(date: LocalDate): String = MyDate.fromLocalDate(date).dateString
}
