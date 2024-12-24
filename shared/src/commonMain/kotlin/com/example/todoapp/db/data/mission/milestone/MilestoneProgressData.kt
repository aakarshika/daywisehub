package com.example.todoapp.db.data.mission.milestone
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.models.MyDate


@Entity(
    tableName = "milestone_progress_data",
    foreignKeys = [ForeignKey(
        entity = Milestone::class,
        parentColumns = ["milestone_id"],
        childColumns = ["mpd_milestone_id"],
    )],
    indices = [Index(value = ["mpd_milestone_id"])]
)
data class MilestoneProgressData(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "milestone_progress_data_id") val milestoneProgressDataId: Long = 0,
    @ColumnInfo(name = "mpd_milestone_id") val milestoneId: Long,
    @ColumnInfo(name = "progress_value") val progressValue: Float,
    @ColumnInfo(name = "progress_unit") val progressUnit: Float,
    @ColumnInfo(name = "progress_text") val progressText: String,
    @ColumnInfo(name = "mile_start_date") val mileStartDate: MyDate,
    @ColumnInfo(name = "actual_completion_date") val actualCompletionDate: MyDate?,
    @ColumnInfo(name = "completion_task_id") val completionTaskId: Long?
)
