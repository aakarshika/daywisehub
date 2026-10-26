package com.example.todoapp.db.data.mission.milestone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.todoapp.db.data.mission.Mission
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

    /** Upsert, not REPLACE, as in MissionDao: REPLACE would delete the row and cascade-delete its tasks. */
    @Upsert
    suspend fun upsertMission(mission: Mission): Long

    /**
     * The editor's save, all or none: the mission, then its milestones. [deleted] go, [updated] get the
     * editor's fields only (updateDetails), and [added] go in under the mission's id. Returns that id.
     */
    @Transaction
    suspend fun saveWithMilestones(mission: Mission, deleted: List<Long>, updated: List<Milestone>, added: List<Milestone>): Long {
        // Room's upsert gives -1 on an update.
        val id = upsertMission(mission).let { if (it == -1L) mission.missionId else it }
        deleted.forEach { delete(it) }
        updated.forEach { updateDetails(it.milestoneId, it.text, it.milestoneOrder, it.expectedCompletionDate) }
        added.forEach { insert(it.copy(missionId = id)) }
        return id
    }
}
