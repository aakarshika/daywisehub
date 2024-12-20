// data/User.kt
package com.example.todoapp.db.data.mission

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todoapp.db.models.MyDate


@Entity(tableName = "mission")
data class Mission(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "mission_id") val missionId: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "mission_title") val missionTitle: String,
    @ColumnInfo(name = "importance_value") val importanceValue: Float,
    @ColumnInfo(name = "importance_set") val importanceSet: String,//auto fill
    @ColumnInfo(name = "frequency_set_value") val frequencySetValue: Float,
    @ColumnInfo(name = "frequency_set") val frequencySet: String,//auto fill
    @ColumnInfo(name = "progress_indicator") val progressIndicator: String,
    @ColumnInfo(name = "mission_motivation") val missionMotivation: String?,
    @ColumnInfo(name = "progress_init_value") val progressInitValue: Float?,
    @ColumnInfo(name = "progress_init_text") val progressInitText: String?,
    @ColumnInfo(name = "progress_Unit") val progressUnit: String?,
    @ColumnInfo(name = "start_date") val startDate: MyDate,
    @ColumnInfo(name = "end_date") val endDate: MyDate?,
    @ColumnInfo(name = "mission_hidden_type") val missionHiddenType: String
)
