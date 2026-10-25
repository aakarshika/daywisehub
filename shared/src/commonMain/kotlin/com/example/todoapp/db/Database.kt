package com.example.todoapp.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionDao
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.data.mood.Mood
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodDao
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.plan.PlanDao
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.data.water.WaterIntakeDao

// Version 5 replaces the old mission model. Builders use fallbackToDestructiveMigration, so older databases are wiped.
@Database(
    entities = [
        Mission::class,
        Milestone::class,
        TodayTask::class,
        DayPlan::class,
        DaySettings::class,
        Mood::class,
        TodayMood::class,
        WaterIntake::class],
    version = 5
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getMissionDao(): MissionDao
    abstract fun getMilestoneDao(): MilestoneDao
    abstract fun getTodayTaskDao(): TodayTaskDao
    abstract fun getPlanDao(): PlanDao
    abstract fun getTodayMoodDao(): TodayMoodDao
    abstract fun getWaterIntakeDao(): WaterIntakeDao
}
