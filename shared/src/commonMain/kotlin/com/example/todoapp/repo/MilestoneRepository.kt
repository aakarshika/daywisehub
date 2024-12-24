package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.screen.init.UserInitManager
import kotlinx.coroutines.flow.Flow

class MilestoneRepository(private val database: AppDatabase) {
    private val milestoneDao: MilestoneDao by lazy {
        database.getMilestoneDao()
    }


    suspend fun getAllMilestonesForDate(date: String): Flow<List<MilestoneWithDetails>> {
        return milestoneDao.getAllMilestonesForDate(UserInitManager.getUserId(), date)
    }
    suspend fun getAllMilestonesForDateAndMission(date: String, missionId:Long): Flow<List<MilestoneWithDetails>> {
        return milestoneDao.getAllMilestonesForDateAndMission(date, missionId)
    }

    suspend fun getAllMilestones(): Flow<List<MilestoneWithDetails>> {
        return milestoneDao.getAllMilestones(UserInitManager.getUserId())
    }
}