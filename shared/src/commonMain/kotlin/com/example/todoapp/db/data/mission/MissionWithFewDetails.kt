package com.example.todoapp.db.data.mission

import androidx.room.Embedded
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar

data class MissionWithFewDetails(
    @Embedded val mission: Mission?,
    @Embedded val pillar: Pillar?,
    @Embedded val missionFrequency: MissionFrequency?,
){
    override fun toString(): String {
        return "mission(" +
                "'m${mission?.missionId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                (missionFrequency?.frequency)+(missionFrequency?.frequencyPeriod)+
                ")"
    }
}
