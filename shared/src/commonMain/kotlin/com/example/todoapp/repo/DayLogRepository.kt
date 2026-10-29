package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.DayLogs
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.water_daily_limit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

/** Moods and water from [from] to [to], for the day badges of days with nothing done. */
class DayLogRepository(private val database: AppDatabase) {

    fun observe(from: LocalDate, to: LocalDate): Flow<DayLogs> {
        val a = MyDate.fromLocalDate(from).dateString
        val b = MyDate.fromLocalDate(to).dateString
        return combine(
            database.getTodayMoodDao().observeActiveBetween(a, b),
            database.getWaterIntakeDao().observeBetween(a, b),
        ) { moods, water ->
            DayLogs(
                moods = moods.groupBy({ it.date.toLocalDate() }, { it.icon }),
                // The header's last glass is index water_daily_limit - 1: filling it fills them all.
                fullWater = water.filter { it.waterIntakeProgress >= water_daily_limit - 1 }.mapTo(mutableSetOf()) { it.waterDate.toLocalDate() },
            )
        }
    }
}
