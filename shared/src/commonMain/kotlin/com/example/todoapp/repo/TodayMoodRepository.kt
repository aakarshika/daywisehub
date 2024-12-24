package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mood.Mood
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodDao
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.user.User
import com.example.todoapp.db.models.MyDate
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
                moodName = "HAPPY",
                moodIcon = "happy"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = user.id,
                moodName = "SAD",
                moodIcon = "sad"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = user.id,
                moodName = "ANGRY",
                moodIcon = "angry"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = user.id,
                moodName = "AFRAID",
                moodIcon = "afraid"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = user.id,
                moodName = "SURPRISE",
                moodIcon = "surprised"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = user.id,
                moodName = "DISGUSTED",
                moodIcon = "disgusted"
            )
        )


    }

}