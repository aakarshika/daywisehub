package com.example.todoapp.db.data.mood

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood")
data class Mood(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "mood_id") val moodId: Long = 0,
    @ColumnInfo(name = "mood_name") val moodName: String?,
    @ColumnInfo(name = "mood_user_id") val moodUserId: Long,
    @ColumnInfo(name = "mood_icon") val moodIcon: String // Holds drawable resource ID
)
