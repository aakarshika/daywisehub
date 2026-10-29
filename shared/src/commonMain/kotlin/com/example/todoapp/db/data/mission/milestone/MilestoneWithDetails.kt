package com.example.todoapp.db.data.mission.milestone

import androidx.room.Embedded
import com.example.todoapp.db.data.mission.Mission

data class MilestoneWithDetails(
    @Embedded val mission: Mission,
    @Embedded val milestone: Milestone
)
