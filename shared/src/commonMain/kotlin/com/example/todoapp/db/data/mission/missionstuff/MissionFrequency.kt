package com.example.todoapp.db.data.mission.missionstuff

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.data.mission.Mission

@Entity(
    tableName = "mission_frequency",
    foreignKeys = [ForeignKey(
        entity = Mission::class,
        parentColumns = ["mission_id"],
        childColumns = ["fs_mission_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["fs_mission_id"])]
)
data class MissionFrequency(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "frequency_set_id") val frequencySetId: Long = 0,
    @ColumnInfo(name = "fs_mission_id") val missionId: Long,
    @ColumnInfo(name = "frequency_set_name") val frequencySetName: String?,
    @ColumnInfo(name = "frequency") val frequency: Int,
    @ColumnInfo(name = "frequency_period") val frequencyPeriod: String?,
    @ColumnInfo(name = "frequency_unit") val frequencyUnit: String?,
    @ColumnInfo(name = "is_daily_habit") val isDailyHabit: Boolean,
    @ColumnInfo(name = "fs_active") val active: String?
)
