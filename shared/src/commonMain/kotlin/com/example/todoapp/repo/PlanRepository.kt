package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.plan.PlanDao
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.History
import com.example.todoapp.domain.PlanInput
import com.example.todoapp.domain.Planner
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.date
import com.example.todoapp.domain.taskState
import com.example.todoapp.domain.weekdayName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class PlanRepository(
    private val database: AppDatabase,
    private val missionRepository: MissionRepository,
    private val todayTaskRepository: TodayTaskRepository,
    private val milestoneRepository: MilestoneRepository,
) {
    private val planDao: PlanDao by lazy { database.getPlanDao() }
    private val mutex = Mutex()

    /** Missions swapped out per day, so "Swap one" doesn't bounce between two. Kept in memory only. */
    private val swappedOut = mutableMapOf<LocalDate, MutableSet<Long>>()

    fun observePlan(date: LocalDate): Flow<DayPlan?> = planDao.observePlan(key(date))

    suspend fun ensureDefaults() {
        if (planDao.getSettings().isEmpty()) planDao.upsertSettings(DaySettings.defaults)
    }

    /** Builds the day's list the first time it's asked for; later calls do nothing. */
    suspend fun ensurePlanned(date: LocalDate) {
        mutex.withLock {
            if (planDao.getPlan(key(date)) != null) return@withLock
            // fill() writes the plan row last, so a build cut short (view model cancelled) is simply redone next time.
            fill(date, DayPlan(date = key(date), freeMinutes = defaultFreeMinutes(date)))
        }
    }

    /** New free time or energy for the day: keeps what's done, crowned, moved in or added by hand, and re-picks the rest. */
    suspend fun changeDay(date: LocalDate, freeMinutes: Int, energy: Energy) {
        mutex.withLock {
            val current = planDao.getPlan(key(date)) ?: DayPlan(date = key(date), freeMinutes = defaultFreeMinutes(date))
            val plan = current.copy(freeMinutes = freeMinutes, energy = energy.name)
            planDao.upsertPlan(plan)
            rebuild(date, plan)
        }
    }

    suspend fun lighterDay(date: LocalDate) {
        val freeMinutes = planDao.getPlan(key(date))?.freeMinutes ?: defaultFreeMinutes(date)
        changeDay(date, freeMinutes, Energy.LOW)
    }

    suspend fun confirm(date: LocalDate) {
        mutex.withLock {
            planDao.getPlan(key(date))?.let { planDao.upsertPlan(it.copy(confirmed = true)) }
        }
    }

    /** Evening check: fixed blocks start ticked, once per day, so an untick sticks. */
    suspend fun tickFixedBlocks(date: LocalDate) {
        mutex.withLock {
            val plan = planDao.getPlan(key(date)) ?: return@withLock
            if (plan.fixedTicked) return@withLock
            todayTaskRepository.getBetween(date, date)
                .filter { it.isFixedBlock && it.taskState == TaskState.PLANNED }
                .forEach { updateState(it, TaskState.DONE) }
            planDao.upsertPlan(plan.copy(fixedTicked = true))
        }
    }

    suspend fun setState(taskId: Long, state: TaskState) {
        mutex.withLock {
            todayTaskRepository.get(taskId)?.let { updateState(it, state) }
        }
    }

    suspend fun toggleDone(taskId: Long) {
        mutex.withLock {
            val task = todayTaskRepository.get(taskId) ?: return@withLock
            updateState(task, if (task.taskState.countsAsDone) TaskState.PLANNED else TaskState.DONE)
        }
    }

    suspend fun toggleCrown(taskId: Long) {
        mutex.withLock {
            val task = todayTaskRepository.get(taskId) ?: return@withLock
            if (!task.crowned) {
                val crownedToday = todayTaskRepository.getBetween(task.date, task.date).count { it.crowned }
                if (crownedToday >= MAX_CROWNS) return@withLock
            }
            todayTaskRepository.update(task.copy(crowned = !task.crowned))
        }
    }

    /** Moves an open task to the next day with room (tomorrow if none in the next week). */
    suspend fun moveToNextRoom(taskId: Long) {
        mutex.withLock {
            val task = todayTaskRepository.get(taskId) ?: return@withLock
            val mission = missionRepository.get(task.missionId) ?: return@withLock
            val from = task.date
            val target = roomByDay(from).entries.firstOrNull { it.value >= mission.durationMinutes }?.key
                ?: from.plus(1, DateTimeUnit.DAY)
            todayTaskRepository.update(task.copy(state = TaskState.MOVED.name, pointsEarned = 0))
            todayTaskRepository.insert(
                newTask(mission, target, Reason(ReasonCode.MOVED, listOf(from.weekdayName())), fixed = false, order = 0, movedFrom = from)
            )
        }
    }

    suspend fun swap(taskId: Long) {
        mutex.withLock {
            val task = todayTaskRepository.get(taskId) ?: return@withLock
            // A moved-in task stays; it can still be moved again or skipped in the evening.
            if (task.movedFromDate != null) return@withLock
            val mission = missionRepository.get(task.missionId) ?: return@withLock
            val date = task.date
            val plan = planDao.getPlan(key(date)) ?: return@withLock
            val excluded = swappedOut.getOrPut(date) { mutableSetOf() }.apply { add(task.missionId) }
            todayTaskRepository.delete(taskId)
            val next = Planner.nextCandidate(input(date, plan), swappedOut = mission, exclude = excluded) ?: return@withLock
            todayTaskRepository.insert(newTask(next.mission, date, next.reason, fixed = false, order = task.sortOrder))
        }
    }

    suspend fun addMission(date: LocalDate, missionId: Long) {
        mutex.withLock {
            val mission = missionRepository.get(missionId) ?: return@withLock
            val order = todayTaskRepository.getBetween(date, date).size
            todayTaskRepository.insert(newTask(mission, date, Reason(ReasonCode.ADDED), fixed = false, order = order))
        }
    }

    suspend fun removeTask(taskId: Long) {
        mutex.withLock { todayTaskRepository.delete(taskId) }
    }

    private suspend fun updateState(task: TodayTask, state: TaskState) {
        val mission = missionRepository.get(task.missionId) ?: return
        todayTaskRepository.update(task.copy(state = state.name, pointsEarned = Points.forTask(mission, state)))
    }

    private suspend fun rebuild(date: LocalDate, plan: DayPlan) {
        todayTaskRepository.getBetween(date, date)
            .filter {
                it.taskState == TaskState.PLANNED && !it.isFixedBlock && !it.crowned &&
                    it.movedFromDate == null && it.reasonCode != ReasonCode.ADDED.name
            }
            .forEach { todayTaskRepository.delete(it.todayTaskId) }
        fill(date, plan)
    }

    private suspend fun fill(date: LocalDate, plan: DayPlan) {
        val input = input(date, plan)
        val result = Planner.plan(input)
        val firstOrder = input.todayTasks.size
        todayTaskRepository.insertAll(
            result.picks.mapIndexed { i, pick -> newTask(pick.mission, date, pick.reason, pick.isFixedBlock, firstOrder + i) }
        )
        planDao.upsertPlan(
            plan.copy(
                didntFitMissionId = result.didntFit?.missionId,
                didntFitDue = result.didntFit?.due?.let { key(it) },
                didntFitRoomDay = result.didntFit?.roomDay?.let { key(it) },
            )
        )
    }

    private suspend fun input(date: LocalDate, plan: DayPlan): PlanInput {
        val missions = missionRepository.getActive()
        val tasks = todayTaskRepository.getBetween(date.minus(HISTORY_DAYS, DateTimeUnit.DAY), date.plus(7, DateTimeUnit.DAY))
        val room = roomByDay(date, missions, tasks)
        return PlanInput(
            date = date,
            missions = missions,
            milestones = milestoneRepository.getAll(),
            history = History(tasks),
            todayTasks = tasks.filter { it.date == date },
            freeMinutes = plan.freeMinutes,
            energy = Energy.valueOf(plan.energy),
            roomOn = { day -> room[day] ?: 0 },
        )
    }

    /** Free minutes left on each of the 7 days after [from], in date order. */
    private suspend fun roomByDay(
        from: LocalDate,
        missions: List<Mission>? = null,
        tasks: List<TodayTask>? = null,
    ): Map<LocalDate, Int> {
        val minutesById = (missions ?: missionRepository.getActive()).associate { it.missionId to it.durationMinutes }
        val days = (1..7).map { from.plus(it, DateTimeUnit.DAY) }
        val upcoming = tasks ?: todayTaskRepository.getBetween(days.first(), days.last())
        val room = LinkedHashMap<LocalDate, Int>()
        for (day in days) {
            val free = planDao.getPlan(key(day))?.freeMinutes ?: defaultFreeMinutes(day)
            val planned = upcoming
                .filter { it.date == day && !it.isFixedBlock && it.taskState.isOpen }
                .sumOf { minutesById[it.missionId] ?: 0 }
            room[day] = free - planned
        }
        return room
    }

    private suspend fun defaultFreeMinutes(date: LocalDate): Int {
        val weekday = date.dayOfWeek.isoDayNumber
        return planDao.getSettings().firstOrNull { it.weekday == weekday }?.freeMinutes
            ?: DaySettings.defaultFor(weekday)
    }

    private fun newTask(
        mission: Mission,
        date: LocalDate,
        reason: Reason,
        fixed: Boolean,
        order: Int,
        movedFrom: LocalDate? = null,
    ) = TodayTask(
        missionId = mission.missionId,
        taskDate = MyDate.fromLocalDate(date),
        isFixedBlock = fixed,
        reasonCode = reason.code.name,
        reasonArgs = reason.encodeArgs(),
        sortOrder = order,
        movedFromDate = movedFrom?.let { MyDate.fromLocalDate(it) },
    )

    private fun key(date: LocalDate): String = MyDate.fromLocalDate(date).dateString

    companion object {
        const val HISTORY_DAYS = 120
        const val MAX_CROWNS = 3
    }
}
