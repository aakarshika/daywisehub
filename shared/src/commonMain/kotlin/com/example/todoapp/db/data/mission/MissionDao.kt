package com.example.todoapp.db.data.mission

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneProgressData
import com.example.todoapp.db.data.mission.milestone.MilestoneWithFewDetails
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.mission.missionstuff.MissionPillarMapping
import com.example.todoapp.db.data.pillar.Pillar
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMission(mission: Mission): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMission(mission: Mission)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPillar(pillar: Pillar)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPillarMapping(missionPillarMapping: MissionPillarMapping)

    //upsert milestone
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestone(milestone: Milestone): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestoneProgressData(milestoneProgressData: MilestoneProgressData): Long

    //delete milestone
    @Query("DELETE FROM milestone WHERE mile_mission_id = :missionId")
    suspend fun deleteMilestones(missionId: Long)
    @Query("DELETE FROM milestone_progress_data WHERE mpd_milestone_id = :missionId")
    suspend fun deleteMilestoneProgressDatas(missionId: Long)

    //delete deletePillarMapping
    @Query("DELETE FROM mission_pillar_mapping WHERE mission_mapping_id = :missionId")
    suspend fun deletePillarMapping(missionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFrequencySet(frequencySet: MissionFrequency)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestones(milestones: List<Milestone>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestoneProgressDatas(milestones: List<MilestoneProgressData>)

    @Transaction
    @Query("""
        SELECT  m.mission_id
        FROM mission m WHERE m.user_id = :userId
    """)
     fun getAllMissionIds(userId: Long): Flow<List<Long>>
    //
    @Transaction
    @Query("SELECT * FROM pillar")
     fun getAllPillars(): Flow<List<Pillar>>


    @Query("SELECT * FROM Mission WHERE mission_id = :missionId")
    fun getMission(missionId: Long): Flow<Mission>

    @Query("""
        SELECT p.* FROM pillar p
        INNER JOIN mission_pillar_mapping mp 
        ON p.pillar_id = mp.pillar_mapping_id 
        WHERE mp.mission_mapping_id = :missionId
    """)
    fun getPillarForMission(missionId: Long): Flow<Pillar?>

    @Query("SELECT * FROM mission_frequency WHERE fs_mission_id = :missionId")
    fun getMissionFrequency(missionId: Long): Flow<MissionFrequency?>

    @Query("""
        SELECT * FROM milestone m
        INNER JOIN milestone_progress_data mp 
        ON m.milestone_id = mp.mpd_milestone_id 
        WHERE m.mile_mission_id = :missionId
        """)
    fun getMilestones(missionId: Long): Flow<List<MilestoneWithFewDetails>?>

    @Transaction
    suspend fun insertFullMission(missionWithDetails: MissionWithDetails): Long {
        Logger.e("MissionDAO")
        Logger.e("$missionWithDetails")

        val mission = missionWithDetails.mission
        val frequencySet = missionWithDetails.missionFrequency
        val milestones = missionWithDetails.milestones
        val pillar = missionWithDetails.pillar

        var missionId :Long = missionWithDetails.mission?.missionId?:0L
        if (mission !=null && mission.missionId != 0L) {
            updateMission(mission)
        } else {
            missionId = addMission(mission!!)
        }

        if (frequencySet != null) {
            upsertFrequencySet(frequencySet.copy(missionId = missionId))
        }

        val mp = pillar?.let { MissionPillarMapping(missionId = missionId, pillarId = it.pillarId) }
        if (pillar != null) {
            if (mp != null) {
                upsertPillarMapping(mp)
            }
        }


        if (milestones != null) {
            updateMilestones(milestones, missionId)
        }
        return missionId
    }

    suspend fun deleteMilestonesss(
        missionId: Long
    ) {
        deleteMilestones(missionId = missionId)
        deleteMilestoneProgressDatas(missionId = missionId)
    }
    suspend fun updateMilestones(
        milestones: List<MilestoneWithFewDetails>,
        missionId: Long
    ) {
        milestones.forEach { mile ->
            val mileId = upsertMilestone(mile.milestone.copy(missionId = missionId))
            upsertMilestoneProgressData(mile.milestoneProgressData.copy(milestoneId = mileId))
        }
    }
}

