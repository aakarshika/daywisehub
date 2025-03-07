package com.example.todoapp.db.data.mission.milestone
import androidx.room.Embedded
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.pillar.Pillar


data class MilestoneWithDetails(
    @Embedded val mission: Mission,
    @Embedded val pillar: Pillar,
    @Embedded val milestone: Milestone
)