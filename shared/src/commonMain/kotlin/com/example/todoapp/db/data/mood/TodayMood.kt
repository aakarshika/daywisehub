package com.example.todoapp.db.data.mood

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.models.MyDate

@Entity(
    tableName = "today_mood",
    foreignKeys =
        [
        ForeignKey(
            entity = Mood::class,
            parentColumns = ["mood_id"],
            childColumns = ["tm_mood_id"],
            onDelete = ForeignKey.CASCADE
            )
        ],
    indices = [Index(value = ["tm_mood_id"])]
)
data class TodayMood(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "tm_id") val tmId: Long = 0,
    @ColumnInfo(name = "tm_mood_id") val tmMoodId: Long,
    @ColumnInfo(name = "tm_mood_status") val tmMoodStatus: String,
    @ColumnInfo(name = "tm_date") val tmDate: MyDate
)
