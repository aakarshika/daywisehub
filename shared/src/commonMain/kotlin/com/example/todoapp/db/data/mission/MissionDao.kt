package com.example.todoapp.db.data.mission

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    /** Upsert, not REPLACE: REPLACE would delete the row and cascade-delete its tasks. */
    @Upsert
    suspend fun upsert(mission: Mission): Long

    @Query("SELECT * FROM mission WHERE mission_active = 1 ORDER BY mission_id")
    fun observeActive(): Flow<List<Mission>>

    @Query("SELECT * FROM mission WHERE mission_active = 1 ORDER BY mission_id")
    suspend fun getActive(): List<Mission>

    /** Archived missions too, for points they earned before they were archived. */
    @Query("SELECT * FROM mission ORDER BY mission_id")
    fun observeAll(): Flow<List<Mission>>

    @Query("SELECT * FROM mission ORDER BY mission_id")
    suspend fun getAll(): List<Mission>

    @Query("SELECT * FROM mission WHERE mission_id = :missionId")
    suspend fun get(missionId: Long): Mission?

    @Query("SELECT COUNT(*) FROM mission")
    suspend fun count(): Int

    /** Archives in one statement, so nothing read earlier can be written back over it. */
    @Query("UPDATE mission SET mission_active = 0 WHERE mission_id = :missionId")
    suspend fun setInactive(missionId: Long)
}
