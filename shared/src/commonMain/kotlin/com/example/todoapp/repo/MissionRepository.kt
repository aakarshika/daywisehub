package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionDao
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.MissionWithFewDetails
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.mission.missionstuff.MissionPillarMapping
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow

class MissionRepository(private val database: AppDatabase) {

    private val missionDao: MissionDao by lazy {
        database.getMissionDao()
    }

    //get mission
    fun getMission(missionId: Long): Flow<Mission> {

        return missionDao.getMission(missionId)
    }

    fun getMissions(): Flow<List<MissionWithFewDetails>> {
        return missionDao.getMissions()
    }

    fun getPillar(missionId: Long): Flow<Pillar?> {
        return  missionDao.getPillarForMission(missionId)
    }

    fun getMissionFrequency(missionId: Long): Flow<MissionFrequency?> {
        return missionDao.getMissionFrequency(missionId)
    }
    //get milestones
    fun getMilestones(missionId: Long): Flow<List<Milestone>?> {
        return missionDao.getMilestones(missionId)
    }

    //get all missions
     fun getAllMissionIds(userId: Long): Flow<List<Long>> {
        return missionDao.getAllMissionIds(userId)
    }

    //insert or update mission
    suspend fun insertFullMission(missionWithDetails: MissionWithDetails): Long {
        return missionDao.insertFullMission(missionWithDetails)
    }

    //update mission
    suspend fun updateMission(mission: Mission) {
        missionDao.updateMission(mission)
    }
    //update mission frequency
    suspend fun updateMissionFrequency(missionFrequency: MissionFrequency, missionId:Long) {
        missionDao.upsertFrequencySet(missionFrequency.copy(
            missionId = missionId
        ))
    }
    //update milestones
    suspend fun updateMilestones(milestones: List<Milestone>, missionId: Long) {
        missionDao.updateMilestones(milestones, missionId = missionId)
    }

    //update pillar mapping
    suspend fun updatePillarMapping(missionId:Long, pillar: Pillar) {
        missionDao.deletePillarMapping(missionId)
        missionDao.upsertPillarMapping(MissionPillarMapping(
            missionId = missionId,
            pillarId = pillar.pillarId
        ))
    }

    suspend fun insertDefaultPillars(userId: Long) {
        missionDao.upsertPillar(
            Pillar(
                pillarId = 1L,
                pillarUserId = userId,
                pillarDescription = "Take care of your body. It is the only one you have!",
                pillarBenefits = "Proper attention to physical wellness at least 3-5 times a week is recommended for our body",
                pillarName = "HEALTH",
                uiData = "health_icon",
                pillarFrequency = 4,
                pillarFrequencyPeriod = "WEEKLY",
                pillarFrequencyUnit = "TIMES",
                pillarRelativeImportance = 10
            )
        )
        missionDao.upsertPillar(
            Pillar(
                pillarId = 2L,
                pillarUserId = userId,
                pillarDescription = "Money isn't everything, but it is some things..",
                pillarBenefits = "Improve your skills, stay connected, and constantly walk towards your dreams",
                pillarName = "WEALTH",
                uiData = "wealth_icon",
                pillarFrequency = 3,
                pillarFrequencyPeriod = "WEEKLY",
                pillarFrequencyUnit = "TIMES",
                pillarRelativeImportance = 7
            )
        )
        missionDao.upsertPillar(
            Pillar(
                pillarId = 3L,
                pillarUserId = userId,
                pillarDescription = "We all need love from our family, friends, neighbours, and internet trolls..",
                pillarBenefits = "Keep your social life alive by making it a priority",
                pillarName = "LOVE",
                uiData = "love_icon",
                pillarFrequency = 1,
                pillarFrequencyPeriod = "DAILY",
                pillarFrequencyUnit = "TIMES",
                pillarRelativeImportance = 10
            )
        )
        missionDao.upsertPillar(
            Pillar(
                pillarId = 4L,
                pillarUserId = userId,
                pillarDescription = "Meditate, Create, Dream, Build, Destroy..",
                pillarBenefits = "Mental Peace needs attention. Multi-dimensional growth is needed to feel sustained and alive",
                pillarName = "LIFE",
                uiData = "life_icon",
                pillarFrequency = 4,
                pillarFrequencyPeriod = "WEEKLY",
                pillarFrequencyUnit = "TIMES",
                pillarRelativeImportance = 8
            )
        )
    }
}