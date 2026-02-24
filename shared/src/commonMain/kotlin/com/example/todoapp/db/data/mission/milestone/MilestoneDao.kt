package com.example.todoapp.db.data.mission.milestone

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {

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
            m.user_id = :userId
            AND mls.expected_completion_date >= :startDateString
            AND mls.expected_completion_date <= :endDateString
        ORDER BY mls.expected_completion_date ASC
    """)
    fun getMilestonesForUserInDateRange(
        userId: Long,
        startDateString: String,
        endDateString: String
    ): Flow<List<MilestoneWithDetails>>

    @Update
    suspend fun updateMilestone(milestone: Milestone)
}
