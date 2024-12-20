package com.example.todoapp.db.data.todotask

import androidx.room.Embedded
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar


data class TodayTaskWithFewDetails(
    @Embedded val todayTask: TodayTask,
    @Embedded val todayTaskReminder: TodayTaskReminder?,
    @Embedded val mission: Mission?,
    @Embedded val pillar: Pillar?,
    @Embedded val missionFrequency: MissionFrequency?
)
data class TodayTaskWithDetails(
    val todayTaskWithFewDetails: TodayTaskWithFewDetails,
    val subTasks: List<SubTask>
)