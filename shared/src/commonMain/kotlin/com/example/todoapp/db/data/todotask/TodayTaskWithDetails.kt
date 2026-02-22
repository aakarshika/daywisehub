package com.example.todoapp.db.data.todotask

import androidx.room.Embedded
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.db.models.TaskType


data class TodayTaskWithFewDetails(
    @Embedded val todayTask: TodayTask,
    @Embedded val todayTaskReminder: TodayTaskReminder?,
    @Embedded val mission: Mission?,
    @Embedded val pillar: Pillar?,
    @Embedded val missionFrequency: MissionFrequency?
) {
    override fun toString(): String {
        return if(missionFrequency?.isDailyHabit == true)
            "habit(" +
                    "'m${mission?.missionId?:0L},t${todayTask?.todayTaskId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                    (todayTask?.taskDate?.dateString?:"")+
                    (if (todayTask?.taskStatus==TaskStatus.COMPLETED.value) "/CMPLD" else "")+
                    (if (todayTask?.taskPageTag==TaskType.TOP3.value) "/T3" else "")+
                    ")"
        else "todo(" +
                "'m${mission?.missionId?:0L},t${todayTask?.todayTaskId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                (todayTask?.taskDate?.dateString?:"")+
                (if (todayTask?.taskStatus==TaskStatus.COMPLETED.value) "/CMPLD" else "")+
                (if (todayTask?.taskPageTag==TaskType.TOP3.value) "/T3" else "")+
                ")"
    }
}


data class TodayTaskWithDetails(
    val todayTaskWithFewDetails: TodayTaskWithFewDetails,
    val subTasks: List<SubTask>
)

data class HabitTaskWithFewDetails(
    @Embedded val mission: Mission,
    @Embedded val todayTask: TodayTask?,
    @Embedded val todayTaskReminder: TodayTaskReminder?,
    @Embedded val pillar: Pillar?,
    @Embedded val missionFrequency: MissionFrequency?
) {
    override fun toString(): String {
        return if(missionFrequency?.isDailyHabit == true)
            "habit(" +
                    "'m${mission?.missionId?:0L},t${todayTask?.todayTaskId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                    (todayTask?.taskDate?.dateString?:"")+
                    (if (todayTask?.taskStatus==TaskStatus.COMPLETED.value) "/CMPLD" else "")+
                    (if (todayTask?.taskPageTag==TaskType.TOP3.value) "/T3" else "")+
                    ")"
        else "todo(" +
                "'m${mission?.missionId?:0L},t${todayTask?.todayTaskId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                (todayTask?.taskDate?.dateString?:"")+
                (if (todayTask?.taskStatus==TaskStatus.COMPLETED.value) "/CMPLD" else "")+
                (if (todayTask?.taskPageTag==TaskType.TOP3.value) "/T3" else "")+
                ")"
    }
}