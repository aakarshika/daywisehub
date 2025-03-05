package com.example.todoapp.db.data.todotask
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.models.MyDate

@Entity(
    tableName = "today_task",
    foreignKeys = [ForeignKey(
        entity = Mission::class,
        parentColumns = ["mission_id"],
        childColumns = ["task_mission_id"]
    )],
    indices = [Index(value = ["task_mission_id"])]
)
data class TodayTask(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "today_task_id") val todayTaskId: Long = 0,
    @ColumnInfo(name = "task_user_id") val userId: Long,
    @ColumnInfo(name = "task_mission_id") val missionId: Long,
    @ColumnInfo(name = "task_date") val taskDate: MyDate,

    @ColumnInfo(name = "task_status") val taskStatus: String?,//suggested, rejected, added, in progress, refreshed, completed.
    @ColumnInfo(name = "task_progress_val") val taskProgressVal: Float,
    @ColumnInfo(name = "task_page_tag") val taskPageTag: String? = "TODO",

    @ColumnInfo(name = "task_type") val taskType: String,// comment, today task, highlight, system generated.
    @ColumnInfo(name = "task_text") val taskText: String?, //task text, picture, link, - for system generated task.
    @ColumnInfo(name = "task_picture_url") val taskPictureUrl: String?,
    @ColumnInfo(name = "task_link") val taskLink: String?
)
