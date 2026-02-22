package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mood.Mood
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.data.water.WaterIntakeDao
import com.example.todoapp.db.data.user.User
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class WaterIntakeRepository(private val database: AppDatabase) {
    private val waterIntakeDao: WaterIntakeDao by lazy {
        database.getWaterIntakeDao()
    }

    //insert
    suspend fun upsertWaterIntake(waterIntake: WaterIntake): Long {
        return waterIntakeDao.upsertWaterIntake(waterIntake)
    }

    suspend fun getWaterIntake(date: LocalDate): Flow<WaterIntake?> {
        val selected_date = MyDate.fromLocalDate(date).dateString
        return waterIntakeDao.getWaterIntake(selected_date)
    }

}