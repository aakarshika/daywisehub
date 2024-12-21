package com.example.todoapp.db.data.todotask

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "today_task_reminder",
    foreignKeys = [ForeignKey(
        entity = TodayTask::class,
        parentColumns = ["today_task_id"],
        childColumns = ["rem_task_id"]
    )],
    indices = [Index(value = ["rem_task_id"])]
)
data class TodayTaskReminder(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "reminder_info_id") val reminderInfoId: Long = 0L,
    @ColumnInfo(name = "rem_task_id") val todayTaskId: Long,
    @ColumnInfo(name = "time_of_day") val timeOfDay: String,
    @ColumnInfo(name = "alarm_tone") val alarmTone: String,
    @ColumnInfo(name = "time_before") val timeBefore: Int,
    @ColumnInfo(name = "time_before_unit") val timeBeforeUnit: Int
)