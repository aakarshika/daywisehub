package com.example.todoapp.db

import androidx.room.AutoMigration
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
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.data.week.WeekDao
import com.example.todoapp.db.data.week.WeekSummary

/**
 * Version 5 replaced the old mission model; anything older is wiped (see the platform DB.kt files).
 * From version 5 on, data is kept: each new version adds an auto migration from the one before,
 * generated from the exported schemas in shared/schemas. Version 6 adds the week tables.
 */
@Database(
    entities = [
        Mission::class,
        Milestone::class,
        TodayTask::class,
        DayPlan::class,
        DaySettings::class,
        Mood::class,
        TodayMood::class,
        WaterIntake::class,
        WeekSummary::class,
        PaceOffer::class],
    version = 6,
    autoMigrations = [AutoMigration(from = 5, to = 6)]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getMissionDao(): MissionDao
    abstract fun getMilestoneDao(): MilestoneDao
    abstract fun getTodayTaskDao(): TodayTaskDao
    abstract fun getPlanDao(): PlanDao
    abstract fun getTodayMoodDao(): TodayMoodDao
    abstract fun getWaterIntakeDao(): WaterIntakeDao
    abstract fun getWeekDao(): WeekDao
}
