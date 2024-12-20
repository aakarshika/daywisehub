package com.example.todoapp.db.data.mission.milestone
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.pillar.Pillar

data class MilestoneWithFewDetails(
    @Embedded val milestone: Milestone,
    @Embedded val milestoneProgressData: MilestoneProgressData
)


data class MilestoneWithDetails(
    @Embedded val mission: Mission,
    @Embedded val pillar: Pillar,
    @Embedded val milestoneWithDetails: MilestoneWithFewDetails
)