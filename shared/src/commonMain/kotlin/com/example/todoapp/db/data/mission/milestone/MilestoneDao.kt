package com.example.todoapp.db.data.mission.milestone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.todoapp.db.models.MyDate
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
        WHERE m.mission_active = 1
          AND mls.expected_completion_date >= :startDate
          AND mls.expected_completion_date <= :endDate
        ORDER BY mls.expected_completion_date ASC
    """)
    fun observeInRange(startDate: String, endDate: String): Flow<List<MilestoneWithDetails>>

    @Query("SELECT * FROM milestone")
    suspend fun getAll(): List<Milestone>

    /** Milestones of active missions, for the Missions screen. */
    @Query("""
        SELECT mls.* FROM milestone mls
        JOIN mission m ON m.mission_id = mls.mile_mission_id
        WHERE m.mission_active = 1
        ORDER BY mls.mile_mission_id, mls.milestone_order
    """)
    fun observeAll(): Flow<List<Milestone>>

    @Query("SELECT * FROM milestone WHERE mile_mission_id = :missionId ORDER BY milestone_order")
    suspend fun getForMission(missionId: Long): List<Milestone>

    @Insert
    suspend fun insert(milestone: Milestone): Long

    @Update
    suspend fun updateMilestone(milestone: Milestone)

    /** The editor's fields only, so a milestone ticked off meanwhile keeps its status and progress. */
    @Query("UPDATE milestone SET text = :text, milestone_order = :order, expected_completion_date = :due WHERE milestone_id = :id")
    suspend fun updateDetails(id: Long, text: String, order: Int, due: MyDate)

    @Query("DELETE FROM milestone WHERE milestone_id = :milestoneId")
    suspend fun delete(milestoneId: Long)
}
