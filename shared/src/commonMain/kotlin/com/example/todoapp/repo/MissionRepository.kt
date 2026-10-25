package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionDao
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.domain.MilestoneDraft
import com.example.todoapp.domain.MissionDrafts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MissionRepository(private val database: AppDatabase) {

    private val missionDao: MissionDao by lazy { database.getMissionDao() }
    private val milestoneDao: MilestoneDao by lazy { database.getMilestoneDao() }
    private val mutex = Mutex()

    fun observeActive(): Flow<List<Mission>> = missionDao.observeActive()

    suspend fun getActive(): List<Mission> = missionDao.getActive()

    suspend fun get(missionId: Long): Mission? = missionDao.get(missionId)

    /** Inserts or updates. Returns the mission's id either way (Room's upsert gives -1 on update). Doesn't lock. */
    suspend fun save(mission: Mission): Long {
        val id = missionDao.upsert(mission)
        return if (id == -1L) mission.missionId else id
    }

    /**
     * Saves a mission from the editor with its milestones, their due dates spread evenly up to the
     * mission's end date. Saved milestones left out of [milestones] are deleted; a kept one keeps its
     * done state. Pass an empty list for anything but a finish line. Returns the mission's id.
     */
    suspend fun saveWithMilestones(mission: Mission, milestones: List<MilestoneDraft>): Long = write {
        val id = save(mission)
        val existing = milestoneDao.getForMission(id)
        val kept = milestones.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() }
        val keptIds = kept.map { it.milestoneId }.filter { it != 0L }.toSet()
        existing.filter { it.milestoneId !in keptIds }.forEach { milestoneDao.delete(it.milestoneId) }
        val dates = MissionDrafts.milestoneDates(mission.startDate.toLocalDate(), mission.endDate?.toLocalDate(), kept.size)
        kept.forEachIndexed { order, draft ->
            val due = MyDate.fromLocalDate(dates[order])
            val old = existing.firstOrNull { draft.milestoneId != 0L && it.milestoneId == draft.milestoneId }
            if (old != null) {
                milestoneDao.updateMilestone(old.copy(text = draft.text, milestoneOrder = order, expectedCompletionDate = due))
            } else {
                milestoneDao.insert(
                    Milestone(
                        missionId = id,
                        text = draft.text,
                        priority = "NORMAL",
                        expectedCompletionDate = due,
                        milestoneOrder = order,
                        status = TaskStatus.IN_PROGRESS.value,
                        progressValue = 0f,
                        progressUnit = 0f,
                        progressText = "",
                        mileStartDate = mission.startDate,
                        actualCompletionDate = null,
                        completionTaskId = null,
                    )
                )
            }
        }
        id
    }

    /** Archives a mission. Doesn't lock: PlanRepository.archiveMission calls it inside its own write. */
    suspend fun setInactive(missionId: Long) {
        missionDao.get(missionId)?.let { missionDao.upsert(it.copy(active = false)) }
    }

    suspend fun count(): Int = missionDao.count()

    /** Editor writes: one at a time, finished even if the screen goes away, and off the main thread. */
    private suspend fun <T> write(block: suspend () -> T): T =
        withContext(NonCancellable + Dispatchers.Default) { mutex.withLock { block() } }
}
