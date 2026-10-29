package com.example.todoapp.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.todoapp.db.data.factor.DayFactor
import com.example.todoapp.db.data.factor.DayFactorDao
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
import com.example.todoapp.db.data.reminder.ReminderDao
import com.example.todoapp.db.data.reminder.ReminderSettings
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.data.water.WaterIntakeDao
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.data.week.WeekDao
import com.example.todoapp.db.data.week.WeekSummary

/**
 * Version 5 replaced the old mission model; anything older is wiped (see the platform DB.kt files).
 * A downgrade (an older build installed over a newer database) wipes everything too. That's a
 * conscious choice: it only happens while testing, and an older build can't read newer tables.
 * From version 5 on, data is kept: each new version adds an auto migration from the one before,
 * generated from the exported schemas in shared/schemas. Version 6 adds the week tables, the
 * mission's pace-since day and the task's auto-ticked flag. Version 7 adds the task's progress.
 * Version 8 adds the day factors. Version 9 adds the plan's projected flag. Version 10 adds the reminder settings.
 *
 * Rules for version 8 and later:
 * - A new column must be nullable, or have `@ColumnInfo(defaultValue = ...)`, so the auto migration can add it.
 * - A rename or a delete needs an `AutoMigrationSpec`, or a manual `Migration`.
 * - Keep every `N.json` in shared/schemas, and never edit one by hand.
 * - Never change an entity at the same version: a device already on that version wouldn't migrate.
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
        PaceOffer::class,
        DayFactor::class,
        ReminderSettings::class],
    version = 10,
    autoMigrations = [
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
    ]
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
    abstract fun getDayFactorDao(): DayFactorDao
    abstract fun getReminderDao(): ReminderDao
}
