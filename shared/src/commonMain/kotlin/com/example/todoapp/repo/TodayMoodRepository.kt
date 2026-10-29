package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.factor.DayFactor
import com.example.todoapp.db.data.mood.Mood
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodDao
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
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

    suspend fun getAllMoods(): Flow<List<TodayMoodWithDetails>> {
        return todayMoodDao.getAllMoods()
    }
    suspend fun getTodayMood(date: LocalDate): Flow<List<TodayMoodWithDetails>> {
        val selected_date = MyDate.fromLocalDate(date).dateString
        return todayMoodDao.getTodayMood(selected_date)
    }

    fun getDayFactors(date: LocalDate): Flow<List<String>> =
        database.getDayFactorDao().observeForDay(MyDate.fromLocalDate(date).dateString)

    suspend fun setDayFactor(date: LocalDate, icon: String, picked: Boolean) {
        val dao = database.getDayFactorDao()
        val myDate = MyDate.fromLocalDate(date)
        if (picked) dao.insert(DayFactor(myDate, icon)) else dao.delete(myDate.dateString, icon)
    }

    suspend fun insertDefaultMoods(userId: Long) {
        if (todayMoodDao.getMoodCount() > 0) return
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = userId,
                moodName = "HAPPY",
                moodIcon = "happy"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = userId,
                moodName = "SAD",
                moodIcon = "sad"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = userId,
                moodName = "ANGRY",
                moodIcon = "angry"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = userId,
                moodName = "DREAMY",
                moodIcon = "dreamy"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = userId,
                moodName = "BORED",
                moodIcon = "bored"
            )
        )
        todayMoodDao.upsertMood(
            Mood(
                moodId = 0L,
                moodUserId = userId,
                moodName = "CONFUSED",
                moodIcon = "confused"
            )
        )
//        todayMoodDao.upsertMood(
//            Mood(
//                moodId = 0L,
//                moodUserId = userId,
//                moodName = "AFRAID",
//                moodIcon = "afraid"
//            )
//        )
//        todayMoodDao.upsertMood(
//            Mood(
//                moodId = 0L,
//                moodUserId = userId,
//                moodName = "SURPRISE",
//                moodIcon = "surprised"
//            )
//        )
//        todayMoodDao.upsertMood(
//            Mood(
//                moodId = 0L,
//                moodUserId = userId,
//                moodName = "DISGUSTED",
//                moodIcon = "disgusted"
//            )
//        )


    }

}