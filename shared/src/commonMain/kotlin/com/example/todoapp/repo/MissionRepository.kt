package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionDao
import kotlinx.coroutines.flow.Flow

class MissionRepository(private val database: AppDatabase) {

    private val missionDao: MissionDao by lazy { database.getMissionDao() }

    fun observeActive(): Flow<List<Mission>> = missionDao.observeActive()

    suspend fun getActive(): List<Mission> = missionDao.getActive()

    suspend fun get(missionId: Long): Mission? = missionDao.get(missionId)

    suspend fun save(mission: Mission): Long = missionDao.upsert(mission)

    suspend fun count(): Int = missionDao.count()
}
