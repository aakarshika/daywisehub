package com.example.todoapp.db.data.todotask

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.models.MyDate

/**
 * One mission on one day. [state] stores a TaskState name. [reasonCode] and [reasonArgs]
 * hold the reason line (domain/ReasonLines.kt), so it can be redrawn later.
 */
@Entity(
    tableName = "today_task",
    foreignKeys = [ForeignKey(
        entity = Mission::class,
        parentColumns = ["mission_id"],
        childColumns = ["task_mission_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["task_mission_id"]), Index(value = ["task_date"])]
)
data class TodayTask(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "today_task_id") val todayTaskId: Long = 0,
    @ColumnInfo(name = "task_mission_id") val missionId: Long,
    @ColumnInfo(name = "task_date") val taskDate: MyDate,
    @ColumnInfo(name = "task_state") val state: String = "PLANNED",
    @ColumnInfo(name = "task_points") val pointsEarned: Int = 0,
    @ColumnInfo(name = "task_fixed_block") val isFixedBlock: Boolean = false,
    @ColumnInfo(name = "task_crowned") val crowned: Boolean = false,
    @ColumnInfo(name = "task_moved_from") val movedFromDate: MyDate? = null,
    @ColumnInfo(name = "task_reason_code") val reasonCode: String,
    @ColumnInfo(name = "task_reason_args") val reasonArgs: String = "",
    @ColumnInfo(name = "task_order") val sortOrder: Int = 0,
)
