package com.example.todoapp.db.data.week

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WeekDao {

    @Upsert
    suspend fun upsertSummary(summary: WeekSummary)

    @Upsert
    suspend fun upsertSummaries(summaries: List<WeekSummary>)

    @Query("SELECT * FROM week_summary WHERE summary_week_start = :weekStart")
    suspend fun getSummary(weekStart: String): WeekSummary?

    @Query("SELECT * FROM week_summary WHERE summary_week_start = :weekStart")
    fun observeSummary(weekStart: String): Flow<WeekSummary?>

    /** Every stored week, oldest first. Plan 4's metrics read this. */
    @Query("SELECT * FROM week_summary ORDER BY summary_week_start")
    fun observeSummaries(): Flow<List<WeekSummary>>

    /** Week starts that already have a summary, from [from] on. Dates are yyyy-MM-dd, so text order is date order. */
    @Query("SELECT summary_week_start FROM week_summary WHERE summary_week_start >= :from")
    suspend fun getSummaryWeeksFrom(from: String): List<String>

    /** A second offer for the same mission and week is dropped (the unique index), not an error. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOffers(offers: List<PaceOffer>)

    @Update
    suspend fun updateOffer(offer: PaceOffer)

    @Query("SELECT * FROM pace_offer WHERE offer_id = :offerId")
    suspend fun getOffer(offerId: Long): PaceOffer?

    @Query("SELECT * FROM pace_offer WHERE offer_week = :week ORDER BY offer_id")
    suspend fun getOffers(week: String): List<PaceOffer>

    @Query("SELECT * FROM pace_offer WHERE offer_week = :week ORDER BY offer_id")
    fun observeOffers(week: String): Flow<List<PaceOffer>>

    @Query("SELECT * FROM pace_offer ORDER BY offer_week, offer_id")
    suspend fun getAllOffers(): List<PaceOffer>
}
