package com.example.todoapp.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.todoapp.db.data.loginstatus.LoginStatus
import com.example.todoapp.db.data.loginstatus.LoginStatusDao
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionDao
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.data.mission.milestone.MilestoneProgressData
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.mission.missionstuff.MissionPillarMapping
import com.example.todoapp.db.data.mood.Mood
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodDao
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.SubTask
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.data.user.User
import com.example.todoapp.db.data.user.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [
    User::class,
    Mission::class,
    Pillar::class,
    MissionPillarMapping::class,
    MissionFrequency::class,
    Milestone::class,
    MilestoneProgressData::class,
    TodayTask::class,
    TodayTaskReminder::class,
    SubTask::class,
    Mood::class,
    TodayMood::class,
    LoginStatus::class],
    version = 1
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getUserDao(): UserDao
    abstract fun getLoginStatusDao(): LoginStatusDao
    abstract fun getMissionDao(): MissionDao
    abstract fun getMilestoneDao(): MilestoneDao
    abstract fun getTodayTaskDao(): TodayTaskDao
    abstract fun getTodayMoodDao(): TodayMoodDao

}
