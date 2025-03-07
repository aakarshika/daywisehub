package com.example.todoapp.db.data.mission

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.mission.missionstuff.MissionPillarMapping
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.models.MyDate
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


    //delete milestone
    @Query("DELETE FROM milestone WHERE mile_mission_id = :missionId")
    suspend fun deleteMilestones(missionId: Long)

    //delete deletePillarMapping
    @Query("DELETE FROM mission_pillar_mapping WHERE mission_mapping_id = :missionId")
    suspend fun deletePillarMapping(missionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFrequencySet(frequencySet: MissionFrequency)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestones(milestones: List<Milestone>)

//    @Transaction
//    @Query("""SELECT m.*,p.*,mf.*,mil.*,tt.*
//        FROM mission m
//        LEFT JOIN mission_pillar_mapping mp ON m.mission_id = mp.mission_mapping_id
//        LEFT JOIN pillar p ON mp.pillar_mapping_id = p.pillar_id
//        LEFT JOIN mission_frequency mf ON m.mission_id = mf.fs_mission_id
//        LEFT JOIN milestone mil ON mil.mile_mission_id = m.mission_id
//        ---LEFT JOIN today_task  tt ON tt.task_mission_id = m.mission_id and tt.task_date between date(:date,'-1 month') and :date
//        """)
//    fun getMissionsWithTaskHistory(date: String): Flow<List<MissionWithTaskHistory>>

    @Transaction
    @Query("""SELECT m.*,p.*,mf.*
            ---(select count(*) from today_task tt 
           --- where tt.task_mission_id = m.mission_id and 
              ---  tt.task_date between date(:date,'-1 month') and :date) as nTasksCompletedThisMonth,
            ---(select count(*) from today_task tt 
           --- where tt.task_mission_id = m.mission_id and 
              ---  tt.task_date between date(:date,'-1 week') and :date) as nTasksCompletedThisWeek
        FROM mission m
        LEFT JOIN mission_pillar_mapping mp ON m.mission_id = mp.mission_mapping_id 
        LEFT JOIN pillar p ON mp.pillar_mapping_id = p.pillar_id
        LEFT JOIN mission_frequency mf ON m.mission_id = mf.fs_mission_id
        ---LEFT JOIN milestone mil ON mil.mile_mission_id = m.mission_id and mil.expected_completion_date between date(:date,'+2 day') and :date
        """)
    fun getMissions(): Flow<List<MissionWithFewDetails>>

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
        WHERE m.mile_mission_id = :missionId
        """)
    fun getMilestones(missionId: Long): Flow<List<Milestone>>

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
                deletePillarMapping(missionId)
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
    }
    suspend fun updateMilestones(
        milestones: List<Milestone>,
        missionId: Long
    ) {
        deleteMilestonesss(missionId)
        milestones.forEach { mile ->
            val mileId = upsertMilestone(mile.copy(missionId = missionId))
        }
    }
}

