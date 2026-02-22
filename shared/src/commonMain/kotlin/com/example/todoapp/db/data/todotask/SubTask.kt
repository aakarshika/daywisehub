package com.example.todoapp.db.data.todotask

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sub_task",
    foreignKeys = [ForeignKey(
        entity = TodayTask::class,
        parentColumns = ["today_task_id"],
        childColumns = ["sub_today_task_id"]
    )],
    indices = [Index(value = ["sub_today_task_id"])]
)
data class SubTask(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "task_sub_work_id") val taskSubWorkId: Long = 0L,
    @ColumnInfo(name = "sub_today_task_id") val todayTaskId: Long,
    @ColumnInfo(name = "sub_work_text") val workText: String,
    @ColumnInfo(name = "sub_work_type") val workType: String,
    @ColumnInfo(name = "sub_milestone_id") val milestoneId: Long,
    @ColumnInfo(name = "sub_milestone_status") val milestoneStatus: String,
)