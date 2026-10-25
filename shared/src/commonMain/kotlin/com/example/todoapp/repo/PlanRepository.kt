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
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.PlanInput
import com.example.todoapp.domain.Planner
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.date
import com.example.todoapp.domain.missionKind
import com.example.todoapp.domain.taskState
import com.example.todoapp.domain.weekdayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

    fun observeSettings(): Flow<List<DaySettings>> = planDao.observeSettings()

    suspend fun ensureDefaults() {
        if (planDao.getSettings().isEmpty()) planDao.upsertSettings(DaySettings.defaults)
    }

    /** Builds the day's list the first time it's asked for; later calls do nothing. */
    suspend fun ensurePlanned(date: LocalDate) = write {
        // Fixed blocks start ticked, so earlier days this week score even if their evening check never opened.
        var day = WeekMath.weekStart(date)
        while (day < date) { tickBlocks(day); day = day.plus(1, DateTimeUnit.DAY) }
        if (planDao.getPlan(key(date)) != null) return@write
        // fill() writes the plan row last, so a build cut short is simply redone next time.
        fill(date, DayPlan(date = key(date), freeMinutes = defaultFreeMinutes(date)))
    }

    /** New free time or energy for the day: keeps what's done, crowned, moved in or added by hand, and re-picks the rest. */
    suspend fun changeDay(date: LocalDate, freeMinutes: Int, energy: Energy) = write {
        if (date < today()) return@write
        rebuild(date, currentPlan(date).copy(freeMinutes = freeMinutes.coerceIn(0, MAX_FREE_MINUTES), energy = energy.name))
    }

    suspend fun lighterDay(date: LocalDate) = write {
        if (date < today()) return@write
        rebuild(date, currentPlan(date).copy(energy = Energy.LOW.name))
    }

    suspend fun confirm(date: LocalDate) = write {
        planDao.getPlan(key(date))?.let { planDao.upsertPlan(it.copy(confirmed = true)) }
    }

    /** Evening check: fixed blocks start ticked, once per day, so an untick sticks. */
    suspend fun tickFixedBlocks(date: LocalDate) = write { tickBlocks(date) }

    suspend fun setState(taskId: Long, state: TaskState) = write {
        todayTaskRepository.get(taskId)?.let { updateState(it, state) }
    }

    suspend fun toggleDone(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        updateState(task, if (task.taskState.countsAsDone) TaskState.PLANNED else TaskState.DONE)
    }

    suspend fun toggleCrown(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        if (!task.crowned) {
            if (task.isFixedBlock || !task.taskState.isOpen) return@write
            val crownedToday = todayTaskRepository.getBetween(task.date, task.date).count { it.crowned && it.taskState.isOpen }
            if (crownedToday >= MAX_CROWNS) return@write
        }
        todayTaskRepository.update(task.copy(crowned = !task.crowned))
    }

    /** Moves a planned pick to the next day with room, from today on (the first of those days if none in the week). */
    suspend fun moveToNextRoom(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        if (task.taskState != TaskState.PLANNED || task.isFixedBlock) return@write
        val mission = missionRepository.get(task.missionId) ?: return@write
        val from = task.date
        val after = maxOf(from, today().minus(1, DateTimeUnit.DAY))
        val upcoming = todayTaskRepository.getBetween(after.plus(1, DateTimeUnit.DAY), after.plus(7, DateTimeUnit.DAY))
        val target = roomByDay(after, tasks = upcoming).entries
            .firstOrNull { (day, room) ->
                room >= mission.durationMinutes &&
                    upcoming.none { it.date == day && it.missionId == mission.missionId && it.taskState.isOpen }
            }?.key
            ?: after.plus(1, DateTimeUnit.DAY)
        val order = (upcoming.filter { it.date == target }.maxOfOrNull { it.sortOrder } ?: -1) + 1
        todayTaskRepository.update(task.copy(state = TaskState.MOVED.name, pointsEarned = 0, crowned = false))
        todayTaskRepository.insert(
            newTask(mission, target, Reason(ReasonCode.MOVED, listOf(from.weekdayName())), fixed = false, order = order, movedFrom = from)
        )
    }

    /** True when the item was replaced; false when it stays (nothing fits, crowned, moved in, or not a plain pick). */
    suspend fun swap(taskId: Long): Boolean = writeFor {
        val task = todayTaskRepository.get(taskId) ?: return@writeFor false
        if (task.taskState != TaskState.PLANNED || task.isFixedBlock || task.crowned) return@writeFor false
        // A moved-in task stays; it can still be moved again or skipped in the evening.
        if (task.movedFromDate != null) return@writeFor false
        val mission = missionRepository.get(task.missionId) ?: return@writeFor false
        val date = task.date
        val plan = planDao.getPlan(key(date)) ?: return@writeFor false
        // Pick the replacement first; if nothing fits, the item stays.
        val full = input(date, plan)
        val withoutIt = full.copy(todayTasks = full.todayTasks.filter { it.todayTaskId != taskId })
        val excluded = swappedOut[date].orEmpty() + task.missionId
        val next = Planner.nextCandidate(withoutIt, swappedOut = mission, exclude = excluded) ?: return@writeFor false
        todayTaskRepository.delete(taskId)
        todayTaskRepository.insert(newTask(next.mission, date, next.reason, fixed = false, order = task.sortOrder))
        // Only a real swap keeps the mission out for the rest of the day.
        swappedOut.getOrPut(date) { mutableSetOf() }.add(task.missionId)
        true
    }

    suspend fun addMission(date: LocalDate, missionId: Long) = write {
        val mission = missionRepository.get(missionId) ?: return@write
        if (mission.missionKind == MissionKind.BLOCK) return@write
        val dayTasks = todayTaskRepository.getBetween(date, date)
        if (dayTasks.any { it.missionId == missionId }) return@write
        val order = (dayTasks.maxOfOrNull { it.sortOrder } ?: -1) + 1
        todayTaskRepository.insert(newTask(mission, date, Reason(ReasonCode.ADDED), fixed = false, order = order))
    }

    suspend fun removeTask(taskId: Long) = write { todayTaskRepository.delete(taskId) }

    /** Free minutes per weekday (ISO numbers). Days already planned keep theirs; today changes from the diary. */
    suspend fun saveSettings(minutesByWeekday: Map<Int, Int>) = write {
        planDao.upsertSettings(
            (1..7).map { day ->
                DaySettings(day, (minutesByWeekday[day] ?: DaySettings.defaultFor(day)).coerceIn(0, MAX_FREE_MINUTES))
            }
        )
    }

    /** After a mission is saved: re-picks today's open items, unless the day was confirmed with "Looks good". */
    suspend fun refreshToday() = write { refreshTodayIfOpen() }

    /**
     * Archives a mission: it leaves the Missions screen and the planner, and its still-planned
     * items from today on are removed. Then today is re-picked as in [refreshToday]; a failed re-pick
     * doesn't undo the archive, so it doesn't report failure.
     */
    suspend fun archiveMission(missionId: Long) = write {
        missionRepository.setInactive(missionId)
        todayTaskRepository.deletePlannedFrom(missionId, today())
        runCatching { refreshTodayIfOpen() }
    }

    /** All writes: one at a time, finished even if the caller's screen goes away, and off the main thread. */
    private suspend fun write(block: suspend () -> Unit) = writeFor(block)

    /** [write] for a block that returns a value. */
    private suspend fun <T> writeFor(block: suspend () -> T): T =
        withContext(NonCancellable + Dispatchers.Default) { mutex.withLock { block() } }

    /**
     * Evening check: ticks the day's planned fixed blocks, once per day, so an untick sticks.
     * Does nothing for a day without a plan. Doesn't take the lock, so call it only inside [write].
     */
    private suspend fun tickBlocks(date: LocalDate) {
        val plan = planDao.getPlan(key(date)) ?: return
        if (plan.fixedTicked) return
        todayTaskRepository.getBetween(date, date)
            .filter { it.isFixedBlock && it.taskState == TaskState.PLANNED }
            .forEach { updateState(it, TaskState.DONE) }
        planDao.upsertPlan(plan.copy(fixedTicked = true))
    }

    /**
     * Rebuilds today's open picks when today has a plan that isn't confirmed. Today's still-planned
     * fixed blocks go too, so fill() adds back every block due today with its current days and times;
     * ticked ones stay. Call only inside [write].
     */
    private suspend fun refreshTodayIfOpen() {
        val date = today()
        val plan = planDao.getPlan(key(date)) ?: return
        if (plan.confirmed) return
        todayTaskRepository.getBetween(date, date)
            .filter { it.isFixedBlock && it.taskState == TaskState.PLANNED }
            .forEach { todayTaskRepository.delete(it.todayTaskId) }
        rebuild(date, plan)
    }

    private suspend fun currentPlan(date: LocalDate): DayPlan =
        planDao.getPlan(key(date)) ?: DayPlan(date = key(date), freeMinutes = defaultFreeMinutes(date))

    private fun today(): LocalDate = MyDate.now().toLocalDate()

    private suspend fun updateState(task: TodayTask, state: TaskState) {
        val mission = missionRepository.get(task.missionId) ?: return
        todayTaskRepository.update(
            task.copy(state = state.name, pointsEarned = Points.forTask(mission, state), crowned = task.crowned && state.isOpen)
        )
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
        // A mission swapped out today stays out of the new picks, but still counts for budget and pillar gaps.
        val result = Planner.plan(input, exclude = swappedOut[date].orEmpty())
        val firstOrder = (input.todayTasks.maxOfOrNull { it.sortOrder } ?: -1) + 1
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
            earnedMissions = missionRepository.getAll(),
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
        /** Same cap as the diary's "Your day" dialog. */
        const val MAX_FREE_MINUTES = 16 * 60
    }
}
