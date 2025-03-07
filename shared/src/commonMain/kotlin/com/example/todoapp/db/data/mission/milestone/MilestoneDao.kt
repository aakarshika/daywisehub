package com.example.todoapp.db.data.mission.milestone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestone(milestone: Milestone): Long


    @Query("""
        SELECT mls.* 
        FROM milestone mls 
        WHERE milestone_id = :milestoneId ORDER BY milestone_order ASC 
    """)
     fun getMilestoneById(milestoneId: Long): Flow<Milestone?>

    @Query("""
        SELECT 
            mls.* 
            FROM milestone mls 
        WHERE mls.mile_mission_id = :missionId ORDER BY milestone_order ASC 
    """)
    fun getMilestonesByMissionId(missionId: Long): Flow<List<Milestone>>

    @Query("""
        SELECT 
        mls.*,
        m.*,
        p.*
        FROM milestone mls 
        
        LEFT JOIN mission m ON m.mission_id = mls.mile_mission_id
        LEFT JOIN mission_pillar_mapping pm ON pm.mission_mapping_id = m.mission_id
        LEFT JOIN pillar p ON p.pillar_id = pm.pillar_mapping_id
        WHERE 
            m.user_id = :userId and 
            (mls.actual_completion_date = :date or mls.expected_completion_date = :date )
            """
    )
    fun getAllMilestonesForDate(userId: Long, date: String): Flow<List<MilestoneWithDetails>>

    @Query("""
        SELECT 
        mls.*,
        m.*,
        p.*
        FROM milestone mls 
        
        LEFT JOIN mission m ON m.mission_id = mls.mile_mission_id
        LEFT JOIN mission_pillar_mapping pm ON pm.mission_mapping_id = m.mission_id
        LEFT JOIN pillar p ON p.pillar_id = pm.pillar_mapping_id
        WHERE 
            m.mission_id = :missionId 
            """
    )
    fun getAllMilestonesForMission (missionId:Long): Flow<List<MilestoneWithDetails>>

    @Query("""
        SELECT 
        mls.*,
        m.*,
        p.*
        FROM milestone mls 
        
        LEFT JOIN mission m ON m.mission_id = mls.mile_mission_id
        LEFT JOIN mission_pillar_mapping pm ON pm.mission_mapping_id = m.mission_id
        LEFT JOIN pillar p ON p.pillar_id = pm.pillar_mapping_id
        WHERE 
            m.mission_id = :missionId and 
            (mls.actual_completion_date = :date or mls.expected_completion_date = :date )
            """
    )
    fun getAllMilestonesForDateAndMission (date: String, missionId:Long): Flow<List<MilestoneWithDetails>>
    @Query("""
        SELECT 
        mls.*,
        m.*,
        p.*
        FROM milestone mls 
        
        LEFT JOIN mission m ON m.mission_id = mls.mile_mission_id
        LEFT JOIN mission_pillar_mapping pm ON pm.mission_mapping_id = m.mission_id
        LEFT JOIN pillar p ON p.pillar_id = pm.pillar_mapping_id
        WHERE 
            m.user_id = :userId 
            """
    )
    fun getAllMilestones(userId: Long): Flow<List<MilestoneWithDetails>>

}
