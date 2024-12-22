package com.example.todoapp.db.data.mission

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.todoapp.db.data.mission.milestone.MilestoneWithFewDetails
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.mission.missionstuff.MissionPillarMapping
import com.example.todoapp.db.data.pillar.Pillar


data class MissionWithDetails(
    @Embedded val mission: Mission?,
    @Embedded val pillar: Pillar?,
    @Embedded val missionFrequency: MissionFrequency?,
    @Embedded val milestones: List<MilestoneWithFewDetails>? = listOf(),
){
    override fun toString(): String {
        return if(missionFrequency?.isDailyHabit == true)
            "hMission(" +
                    "'m${mission?.missionId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                    (missionFrequency?.frequency)+(missionFrequency?.frequencyPeriod)+
                    ")"
        else "mission(" +
                "'m${mission?.missionId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                (missionFrequency?.frequency)+(missionFrequency?.frequencyPeriod)+
                ")"
    }
}
