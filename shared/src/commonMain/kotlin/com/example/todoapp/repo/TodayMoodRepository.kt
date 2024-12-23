package com.example.todoapp.repo

import co.touchlab.kermit.Logger
import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mood.Mood
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodDao
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.HabitTaskWithFewDetails
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.data.todotask.TodayTaskWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.data.user.User
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.globalViewModels.UserInitManager
import com.example.todoapp.screen.metrics.ComboTask
import com.kizitonwose.calendar.core.minusDays
import com.kizitonwose.calendar.core.plusDays
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class TodayMoodRepository(private val database: AppDatabase) {
    private val todayMoodDao: TodayMoodDao by lazy {
        database.getTodayMoodDao()
    }

    //insert
    suspend fun upsertTodayMood(todayMood: TodayMood): Long {
        return todayMoodDao.upsertTodayMood(todayMood)
    }

    suspend fun getTodayMood(date: LocalDate): Flow<List<TodayMoodWithDetails>> {
        val selected_date = MyDate.fromLocalDate(date).dateString
        return todayMoodDao.getTodayMood(selected_date)
    }

    suspend fun insertDefaultMoods(user: User) {
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = user.id,
                moodName = "HAPPY"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = user.id,
                moodName = "SAD"
            )
        )

    }

}