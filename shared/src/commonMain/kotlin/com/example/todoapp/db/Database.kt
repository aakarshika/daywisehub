package com.example.todoapp.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionDao
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.mission.missionstuff.MissionPillarMapping
import com.example.todoapp.db.data.mood.Mood
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodDao
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.data.water.WaterIntakeDao
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.SubTask
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [
    Mission::class,
    Pillar::class,
    MissionPillarMapping::class,
    MissionFrequency::class,
    Milestone::class,
    TodayTask::class,
    TodayTaskReminder::class,
    SubTask::class,
    Mood::class,
    TodayMood::class,
    WaterIntake::class],
    version = 4
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getMissionDao(): MissionDao
    abstract fun getMilestoneDao(): MilestoneDao
    abstract fun getTodayTaskDao(): TodayTaskDao
    abstract fun getTodayMoodDao(): TodayMoodDao
    abstract fun getWaterIntakeDao(): WaterIntakeDao

}

// Sign-up/login was removed; drop the tables that backed it without touching user data.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `users`")
        connection.execSQL("DROP TABLE IF EXISTS `login_status`")
    }
}
