package com.example.todoapp.db.data.mission.milestone

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {

    @Query("""
        SELECT mls.*, m.* FROM milestone mls
        JOIN mission m ON m.mission_id = mls.mile_mission_id
        WHERE m.mission_id = :missionId
        ORDER BY mls.milestone_order
    """)
    fun observeForMission(missionId: Long): Flow<List<MilestoneWithDetails>>

    @Query("""
        SELECT mls.*, m.* FROM milestone mls
        JOIN mission m ON m.mission_id = mls.mile_mission_id
        WHERE mls.expected_completion_date >= :startDate
          AND mls.expected_completion_date <= :endDate
        ORDER BY mls.expected_completion_date ASC
    """)
    fun observeInRange(startDate: String, endDate: String): Flow<List<MilestoneWithDetails>>

    @Query("SELECT * FROM milestone")
    suspend fun getAll(): List<Milestone>

    @Update
    suspend fun updateMilestone(milestone: Milestone)
}
