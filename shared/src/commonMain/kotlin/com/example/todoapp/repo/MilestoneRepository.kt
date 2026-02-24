package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow

class MilestoneRepository(private val database: AppDatabase) {
    private val milestoneDao: MilestoneDao by lazy {
        database.getMilestoneDao()
    }

    suspend fun getAllMilestonesForMission(missionId:Long): Flow<List<MilestoneWithDetails>> {
        return milestoneDao.getAllMilestonesForMission(missionId)
    }

    fun getMilestonesForUserInDateRange(
        userId: Long,
        startDate: MyDate,
        endDate: MyDate
    ): Flow<List<MilestoneWithDetails>> {
        return milestoneDao.getMilestonesForUserInDateRange(
            userId = userId,
            startDateString = startDate.dateString,
            endDateString = endDate.dateString
        )
    }

    suspend fun updateMilestone(milestone: Milestone) {
        milestoneDao.updateMilestone(milestone)
    }
}
