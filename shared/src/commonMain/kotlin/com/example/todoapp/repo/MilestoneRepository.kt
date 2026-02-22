package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import kotlinx.coroutines.flow.Flow

class MilestoneRepository(private val database: AppDatabase) {
    private val milestoneDao: MilestoneDao by lazy {
        database.getMilestoneDao()
    }

    suspend fun getAllMilestonesForMission(missionId:Long): Flow<List<MilestoneWithDetails>> {
        return milestoneDao.getAllMilestonesForMission(missionId)
    }
}
