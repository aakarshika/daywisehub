package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.data.week.WeekDao
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Week summaries and pace offers. Nothing here locks: PlanRepository calls the writes inside its own
 * write, together with the task and mission changes they go with.
 */
class WeekRepository(private val database: AppDatabase) {

    private val weekDao: WeekDao by lazy { database.getWeekDao() }

    suspend fun getSummary(weekStart: LocalDate): WeekSummary? = weekDao.getSummary(key(weekStart))

    fun observeSummary(weekStart: LocalDate): Flow<WeekSummary?> = weekDao.observeSummary(key(weekStart))

    /** Every stored week, oldest first. */
    fun observeSummaries(): Flow<List<WeekSummary>> = weekDao.observeSummaries()

    /** Mondays from [from] on that already have a summary. */
    suspend fun summaryWeeksFrom(from: LocalDate): Set<LocalDate> =
        weekDao.getSummaryWeeksFrom(key(from)).map { MyDate(it).toLocalDate() }.toSet()

    suspend fun saveSummary(summary: WeekSummary) = weekDao.upsertSummary(summary)

    suspend fun saveSummaries(summaries: List<WeekSummary>) = weekDao.upsertSummaries(summaries)

    fun observeOffers(week: LocalDate): Flow<List<PaceOffer>> = weekDao.observeOffers(key(week))

    suspend fun getOffers(week: LocalDate): List<PaceOffer> = weekDao.getOffers(key(week))

    suspend fun getAllOffers(): List<PaceOffer> = weekDao.getAllOffers()

    suspend fun getOffer(offerId: Long): PaceOffer? = weekDao.getOffer(offerId)

    suspend fun insertOffers(offers: List<PaceOffer>) = weekDao.insertOffers(offers)

    suspend fun updateOffer(offer: PaceOffer) = weekDao.updateOffer(offer)

    /** One transaction: [offer]'s answer, the [mission] it changed, and for a pause its planned items from [pausedFrom] on go. */
    suspend fun answerOffer(offer: PaceOffer, mission: Mission, pausedFrom: LocalDate?) =
        weekDao.answerOffer(offer, mission, pausedFrom?.let { key(it) })

    private fun key(date: LocalDate): String = MyDate.fromLocalDate(date).dateString
}
