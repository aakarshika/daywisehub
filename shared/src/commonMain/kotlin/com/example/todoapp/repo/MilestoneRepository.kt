package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow

class MilestoneRepository(private val database: AppDatabase) {

    private val milestoneDao: MilestoneDao by lazy { database.getMilestoneDao() }

    fun observeForMission(missionId: Long): Flow<List<MilestoneWithDetails>> =
        milestoneDao.observeForMission(missionId)

    fun observeInRange(startDate: MyDate, endDate: MyDate): Flow<List<MilestoneWithDetails>> =
        milestoneDao.observeInRange(startDate.dateString, endDate.dateString)

    fun observeActiveWithDetails(): Flow<List<MilestoneWithDetails>> = milestoneDao.observeActiveWithDetails()

    suspend fun getAll(): List<Milestone> = milestoneDao.getAll()

    fun observeAll(): Flow<List<Milestone>> = milestoneDao.observeAll()

    fun observeIncludingArchived(): Flow<List<Milestone>> = milestoneDao.observeIncludingArchived()

    suspend fun getForMission(missionId: Long): List<Milestone> = milestoneDao.getForMission(missionId)

    suspend fun updateMilestone(milestone: Milestone) = milestoneDao.updateMilestone(milestone)
}
