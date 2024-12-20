package com.example.todoapp.db.data.mission.missionstuff

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.pillar.Pillar


@Entity(
    tableName = "mission_pillar_mapping",
    foreignKeys = [
        ForeignKey(
            entity = Mission::class,
            parentColumns = ["mission_id"],
            childColumns = ["mission_mapping_id"],
        ),
        ForeignKey(
            entity = Pillar::class,
            parentColumns = ["pillar_id"],
            childColumns = ["pillar_mapping_id"],
        )
    ],
    indices = [Index(value = ["mission_mapping_id"]), Index(value = ["pillar_mapping_id"])]
)
data class MissionPillarMapping(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "mission_pillar_mapping_id") val missionPillarMappingId: Long = 0,
    @ColumnInfo(name = "mission_mapping_id") val missionId: Long,
    @ColumnInfo(name = "pillar_mapping_id") val pillarId: Long
)
