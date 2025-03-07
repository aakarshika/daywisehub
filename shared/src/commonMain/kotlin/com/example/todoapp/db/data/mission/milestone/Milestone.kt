package com.example.todoapp.db.data.mission.milestone

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.models.MyDate


@Entity(
    tableName = "milestone",
    foreignKeys = [ForeignKey(
        entity = Mission::class,
        parentColumns = ["mission_id"],
        childColumns = ["mile_mission_id"]
    )],
    indices = [Index(value = ["mile_mission_id"])]
)
data class Milestone(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "milestone_id") val milestoneId: Long = 0,
    @ColumnInfo(name = "mile_mission_id") val missionId: Long,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "priority") val priority: String,
    @ColumnInfo(name = "expected_completion_date") val expectedCompletionDate: MyDate,
    @ColumnInfo(name = "milestone_order") val milestoneOrder: Int,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "progress_value") val progressValue: Float,
    @ColumnInfo(name = "progress_unit") val progressUnit: Float,
    @ColumnInfo(name = "progress_text") val progressText: String,
    @ColumnInfo(name = "mile_start_date") val mileStartDate: MyDate,
    @ColumnInfo(name = "actual_completion_date") val actualCompletionDate: MyDate?,
    @ColumnInfo(name = "completion_task_id") val completionTaskId: Long?
)
