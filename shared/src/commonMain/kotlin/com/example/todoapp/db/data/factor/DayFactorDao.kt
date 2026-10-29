package com.example.todoapp.db.data.factor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayFactorDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(factor: DayFactor)

    @Query("DELETE FROM day_factor WHERE df_date = :date AND df_icon = :icon")
    suspend fun delete(date: String, icon: String)

    /** The icon names picked on [date]. */
    @Query("SELECT df_icon FROM day_factor WHERE df_date = :date")
    fun observeForDay(date: String): Flow<List<String>>

    /** The factors picked on each day from [from] to [to]. */
    @Query("SELECT * FROM day_factor WHERE df_date BETWEEN :from AND :to")
    fun observeBetween(from: String, to: String): Flow<List<DayFactor>>
}
