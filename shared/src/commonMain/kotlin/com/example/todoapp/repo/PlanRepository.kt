package com.example.todoapp.repo

import co.touchlab.kermit.Logger
import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.plan.PlanDao
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.DayClose
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.History
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.Moves
import com.example.todoapp.domain.OfferResponse
import com.example.todoapp.domain.OfferType
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.Pick
import com.example.todoapp.domain.PlanInput
import com.example.todoapp.domain.Planner
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskProgress
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekLoad
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.WeekSummaries
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
    private val weekRepository: WeekRepository,
) {
    private val planDao: PlanDao by lazy { database.getPlanDao() }
    private val mutex = Mutex()

    /** Missions swapped out per day, so "Swap one" doesn't bounce between two. Kept in memory only. */
    private val swappedOut = mutableMapOf<LocalDate, MutableSet<Long>>()

    fun observePlan(date: LocalDate): Flow<DayPlan?> = planDao.observePlan(key(date))

    fun observePlans(from: LocalDate, to: LocalDate): Flow<List<DayPlan>> = planDao.observePlansBetween(key(from), key(to))

    fun observeSettings(): Flow<List<DaySettings>> = planDao.observeSettings()

    suspend fun ensureDefaults() {
        if (planDao.getSettings().isEmpty()) planDao.upsertSettings(DaySettings.defaults)
    }

    /**
     * Closes what earlier days left open, then builds the day's list the first time it's asked for.
     * Safe on every open: closing only touches what's still open, and a built day isn't rebuilt.
     */
    suspend fun ensurePlanned(date: LocalDate) = write {
        val today = today()
        // Closes against today at the latest, never a day still to come.
        closePastDays(minOf(date, today))
        // A diary opened just before midnight asks for a day that's over by now; it isn't planned.
        if (date < today) return@write
        if (planDao.getPlan(key(date)) != null) return@write
        // fill() writes the picks and the plan row in one transaction, so a build cut short is simply redone next time.
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

    /** "Did some" (50%) or "Skip today". A moved item stays moved, so a fast second tap after "Move" can't count it twice. */
    suspend fun setState(taskId: Long, state: TaskState) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        if (task.taskState == TaskState.MOVED) return@write
        updateState(task, state)
    }

    /**
     * Ticks or unticks an item. Only a done item unticks: a did-some is part done, so the box ticks it (100%).
     * Ticking a moved item takes the move back while its copy is still planned: the copy goes, and the item
     * is done on its own day. Otherwise a moved item stays as it is.
     */
    suspend fun toggleDone(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        if (task.taskState == TaskState.MOVED) {
            val ahead = todayTaskRepository.getBetween(task.date.plus(1, DateTimeUnit.DAY), maxOf(task.date, today()).plus(7, DateTimeUnit.DAY))
            // The same rule as Moves.canTakeBack, which the diary uses to enable the box.
            val copy = Moves.copyOf(task, ahead)?.takeIf { it.taskState == TaskState.PLANNED } ?: return@write
            val done = withState(task, TaskState.DONE) ?: return@write
            // One transaction, so the item is never done with its copy still ahead, nor moved with nothing ahead.
            todayTaskRepository.takeBack(done, copy.todayTaskId)
            refreshClosedWeek(task.date)
            return@write
        }
        // An earlier day is closed: unticking there skips the item, so the next open doesn't carry it forward again.
        val unticked = if (task.date < today()) TaskState.SKIPPED else TaskState.PLANNED
        updateState(task, if (task.taskState == TaskState.DONE) unticked else TaskState.DONE)
    }

    /**
     * Spec 6.5, the swipe: [percent], in 5% steps, says how much got done. 100 ticks the item, 1–99 makes it a
     * did-some (half points, whatever the percent), and 0 unticks it as [toggleDone] does. A moved item stays
     * moved, and a percent the item already shows changes nothing, so a skipped item isn't reopened by a 0.
     */
    suspend fun setProgress(taskId: Long, percent: Int) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        if (task.taskState == TaskState.MOVED) return@write
        val rounded = TaskProgress.rounded(percent.toFloat())
        if (rounded == TaskProgress.shown(task)) return@write
        updateState(task, TaskProgress.stateFor(rounded, task.date, today()), progress = rounded)
    }

    suspend fun toggleCrown(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        if (!task.crowned) {
            if (task.isFixedBlock || !task.taskState.isOpen) return@write
            if (openCrowns(task) >= MAX_CROWNS) return@write
        }
        todayTaskRepository.update(task.copy(crowned = !task.crowned))
    }

    /** Spec 6.3, the pencil: every live mission not on the day's list, with its reason line. Only reads, so it doesn't lock. */
    suspend fun addOptions(date: LocalDate): List<Pick> = withContext(Dispatchers.Default) {
        Planner.options(input(date, currentPlan(date)))
    }

    /**
     * Moves a planned pick to the next day with room, from today on (the first of those days if none in
     * the week). When the mission isn't live on any of them, the pick is skipped instead.
     */
    suspend fun moveToNextRoom(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        if (task.taskState != TaskState.PLANNED || task.isFixedBlock) return@write
        val mission = missionRepository.get(task.missionId) ?: return@write
        if (!moveTask(task, mission, today())) updateState(task, TaskState.SKIPPED)
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
        // One transaction, so the item never goes without its replacement.
        todayTaskRepository.replace(taskId, newTask(next.mission, date, next.reason, fixed = false, order = task.sortOrder))
        // Only a real swap keeps the mission out for the rest of the day.
        swappedOut.getOrPut(date) { mutableSetOf() }.add(task.missionId)
        true
    }

    /** The pencil. An earlier day is closed, so a mission added there goes in as done, with its points. */
    suspend fun addMission(date: LocalDate, missionId: Long) = write {
        val mission = missionRepository.get(missionId) ?: return@write
        if (mission.missionKind == MissionKind.BLOCK) return@write
        val dayTasks = todayTaskRepository.getBetween(date, date)
        if (dayTasks.any { it.missionId == missionId }) return@write
        val order = (dayTasks.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val task = newTask(mission, date, Reason(ReasonCode.ADDED), fixed = false, order = order)
        if (date < today()) {
            todayTaskRepository.insert(
                task.copy(state = TaskState.DONE.name, pointsEarned = Points.forTask(mission, TaskState.DONE), progress = TaskProgress.FULL)
            )
            refreshClosedWeek(date)
        } else {
            todayTaskRepository.insert(task)
        }
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

    /**
     * After the editor saves a mission. When its pace or weight changed ([paceChanged]), this week's
     * unanswered offers for it are withdrawn: they were judged on the old pace. Then today's open items
     * are re-picked, unless the day was confirmed with "Looks good".
     */
    suspend fun missionSaved(missionId: Long, paceChanged: Boolean) = write {
        if (paceChanged) {
            weekRepository.getOffers(WeekMath.weekStart(today()))
                .filter { it.missionId == missionId && it.response == null }
                .forEach { weekRepository.updateOffer(it.copy(response = OfferResponse.WITHDRAWN.name)) }
        }
        refreshTodayIfOpen()
    }

    /**
     * Archives a mission: it leaves the Missions screen and the planner, and its still-planned
     * items from today on are removed. Then today is re-picked as in [missionSaved]; a failed re-pick
     * doesn't undo the archive, so it doesn't report failure.
     */
    suspend fun archiveMission(missionId: Long) = write {
        missionRepository.setInactive(missionId)
        todayTaskRepository.deletePlannedFrom(missionId, today())
        runCatching { refreshTodayIfOpen() }
            .onFailure { Logger.w(it) { "Today's re-pick failed; today's list stays as it was." } }
    }

    /**
     * Spec 5.2: the answer to a pace offer. Only "yes" or "pause a month" changes the mission, and then
     * today's open picks are redone, as after any mission save. A second tap on the same offer does nothing,
     * and so does an answer its sticky doesn't offer, or any answer once the mission is archived. A "yes"
     * that no longer changes anything leaves the offer unanswered. An accepted pace starts a fresh run of
     * weeks from today (Mission.paceSince).
     */
    suspend fun answerOffer(offerId: Long, response: OfferResponse) = write {
        val offer = weekRepository.getOffer(offerId) ?: return@write
        if (offer.response != null) return@write
        val type = OfferType.entries.firstOrNull { it.name == offer.type } ?: return@write
        val mission = missionRepository.get(offer.missionId)?.takeIf { it.active } ?: return@write
        val today = today()
        if (response !in PaceOffers.answers(mission, type, today)) return@write
        val changed = when (response) {
            OfferResponse.ACCEPTED -> PaceOffers.accepted(mission, type)?.copy(paceSince = key(today)) ?: return@write
            OfferResponse.PAUSED -> PaceOffers.paused(mission, today)
            OfferResponse.KEPT, OfferResponse.NOT_YET, OfferResponse.WITHDRAWN -> null
        }
        val week = MyDate(offer.week).toLocalDate()
        val snoozeUntil = if (response == OfferResponse.NOT_YET) week.plus(PaceOffers.SNOOZE_WEEKS * 7, DateTimeUnit.DAY) else null
        val answered = offer.copy(response = response.name, snoozeUntil = snoozeUntil?.let { key(it) })
        if (changed == null) {
            weekRepository.updateOffer(answered)
            return@write
        }
        // One transaction, so the answer is never kept without its change, nor the change without its answer.
        // A paused mission also leaves today's list and anything moved ahead, as when it's archived.
        weekRepository.answerOffer(answered, changed, pausedFrom = if (response == OfferResponse.PAUSED) today else null)
        // As in archiveMission: a failed re-pick doesn't undo the answer.
        runCatching { refreshTodayIfOpen() }
            .onFailure { Logger.w(it) { "Today's re-pick failed; today's list stays as it was." } }
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
            .forEach { updateState(it, TaskState.DONE, byApp = true) }
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
        rebuild(date, plan, withBlocks = true)
    }

    /**
     * Spec 6.4, at the first open of a new day: fixed blocks on earlier days start ticked, then items
     * still planned there follow their "if a day goes sideways" answer. A day that was never opened has
     * no plan, so nothing is ticked there, but it can still hold items moved into it; those are resolved
     * like any other. Week summaries and offers come last. They're worked out from what's stored, so a
     * failure there doesn't stop today's list: the next open makes them again. Call only inside [write].
     */
    private suspend fun closePastDays(today: LocalDate) {
        planDao.getUntickedBefore(key(today)).forEach { tickBlocks(MyDate(it.date).toLocalDate()) }
        resolveUnfinished(today)
        runCatching { closeWeeks(today) }
            .onFailure { Logger.w(it) { "Week close-out failed; the next open makes the summaries and offers again." } }
    }

    /** Items still planned on earlier days follow "if a day goes sideways". Call only inside [write]. */
    private suspend fun resolveUnfinished(today: LocalDate) {
        val unfinished = todayTaskRepository.getPlannedBefore(today)
        if (unfinished.isEmpty()) return
        val missions = missionRepository.getActive().associateBy { it.missionId }
        // Missions with an open item from today on. Each one carried forward joins them.
        val coming = todayTaskRepository.getBetween(today, today.plus(7, DateTimeUnit.DAY))
            .filter { it.taskState.isOpen }
            .mapTo(mutableSetOf()) { it.missionId }
        // Newest first, so when one mission has several, the latest is the one carried forward.
        for (task in unfinished) {
            val mission = missions[task.missionId]
            val outcome = DayClose.outcome(task, mission, today, alreadyComing = task.missionId in coming)
            if (mission != null && outcome == DayClose.Outcome.MOVE && moveTask(task, mission, today)) {
                coming += mission.missionId
            } else {
                // A skipped item keeps its crown, as in updateState, so ticking it later brings the crown back.
                todayTaskRepository.update(task.copy(state = TaskState.SKIPPED.name, pointsEarned = 0, progress = 0))
            }
        }
    }

    /**
     * Moves [task] to the next day with room after [Moves.anchor]; false, with nothing changed, when the
     * mission isn't live on any of those days. The copy and the moved item are written in one transaction,
     * so a move is never half done. Call only inside [write].
     */
    private suspend fun moveTask(task: TodayTask, mission: Mission, today: LocalDate): Boolean {
        val from = task.date
        val after = Moves.anchor(from, today)
        val upcoming = todayTaskRepository.getBetween(after.plus(1, DateTimeUnit.DAY), after.plus(7, DateTimeUnit.DAY))
        val target = Moves.target(mission, upcoming, roomByDay(after, tasks = upcoming)) ?: return false
        val order = (upcoming.filter { it.date == target }.maxOfOrNull { it.sortOrder } ?: -1) + 1
        todayTaskRepository.move(
            copy = newTask(mission, target, Reason(ReasonCode.MOVED, listOf(from.weekdayName())), fixed = false, order = order, movedFrom = from),
            moved = task.copy(state = TaskState.MOVED.name, pointsEarned = 0, crowned = false, progress = 0),
        )
        return true
    }

    /**
     * Spec 5.1: every finished week since the first item gets a summary, once. Its points are what the
     * week's tasks earned, after the ticks and skips above. When last week is among them, this is the
     * first open of a new week, which also makes this week's pace offers (spec 5.2). The offers are
     * written before the summaries, so a close-out cut short is redone next time. Call only inside [write].
     */
    private suspend fun closeWeeks(today: LocalDate) {
        val thisWeek = WeekMath.weekStart(today)
        val lastWeek = thisWeek.minus(7, DateTimeUnit.DAY)
        val first = todayTaskRepository.firstDate()?.let { WeekMath.weekStart(it) } ?: return
        if (first > lastWeek) return
        val stored = weekRepository.summaryWeeksFrom(first)
        val missing = generateSequence(first) { it.plus(7, DateTimeUnit.DAY) }
            .takeWhile { it <= lastWeek }
            .filter { it !in stored }
            .toList()
        if (missing.isEmpty()) return
        val missions = missionRepository.getActive()
        val allMissions = missionRepository.getAll()
        val from = minOf(missing.first().minus(HISTORY_DAYS, DateTimeUnit.DAY), thisWeek.minus(PaceOffers.HISTORY_DAYS, DateTimeUnit.DAY))
        val history = History(todayTaskRepository.getBetween(from, today.plus(7, DateTimeUnit.DAY)))
        if (lastWeek in missing) makeOffers(thisWeek, today, missions, history)
        // Each week's own items, sliced once, so a long gap doesn't scan the whole history for every week.
        val byWeek = history.all.groupBy { WeekMath.weekStart(it.date) }
        weekRepository.saveSummaries(missing.map { WeekSummaries.summarize(it, missions, allMissions, history, byWeek[it].orEmpty()) })
    }

    /** This week's pace offers, unless it already has some. Call only inside [write]. */
    private suspend fun makeOffers(thisWeek: LocalDate, today: LocalDate, missions: List<Mission>, history: History) {
        if (weekRepository.getOffers(thisWeek).isNotEmpty()) return
        val freeLeft = WeekLoad.freeMinutes(planDao.getSettings()) - WeekLoad.plannedMinutes(missions, thisWeek, history)
        val found = PaceOffers.find(thisWeek, today, missions, history, weekRepository.getAllOffers(), freeLeft)
        weekRepository.insertOffers(
            found.map { PaceOffer(missionId = it.mission.missionId, type = it.type.name, week = key(thisWeek)) }
        )
    }

    private suspend fun currentPlan(date: LocalDate): DayPlan =
        planDao.getPlan(key(date)) ?: DayPlan(date = key(date), freeMinutes = defaultFreeMinutes(date))

    private fun today(): LocalDate = MyDate.now().toLocalDate()

    /** Saves [task] in [state] (see [withState]). Call only inside [write]. */
    private suspend fun updateState(task: TodayTask, state: TaskState, byApp: Boolean = false, progress: Int? = null) {
        todayTaskRepository.update(withState(task, state, byApp, progress) ?: return)
        refreshClosedWeek(task.date)
    }

    /**
     * [task] in [state], with its points; null when its mission is gone. Only a move takes its crown: a
     * skipped item keeps it, so ticking it again brings it back, unless the day has filled its
     * [MAX_CROWNS] open crowns meanwhile. [byApp] is true only for the evening check's ticks
     * ("Ticked for you"); any change by hand clears it. Progress follows the state (TaskProgress.stored):
     * a did-some keeps [progress], or 50 without one.
     */
    private suspend fun withState(task: TodayTask, state: TaskState, byApp: Boolean = false, progress: Int? = null): TodayTask? {
        val mission = missionRepository.get(task.missionId) ?: return null
        val reopened = !task.taskState.isOpen && state.isOpen
        val crowned = task.crowned && state != TaskState.MOVED && (!reopened || openCrowns(task) < MAX_CROWNS)
        return task.copy(
            state = state.name,
            pointsEarned = Points.forTask(mission, state),
            crowned = crowned,
            autoTicked = byApp,
            progress = TaskProgress.stored(state, progress),
        )
    }

    /** Crowned items still open on [task]'s day, not counting [task]. */
    private suspend fun openCrowns(task: TodayTask): Int =
        todayTaskRepository.getBetween(task.date, task.date)
            .count { it.todayTaskId != task.todayTaskId && it.crowned && it.taskState.isOpen }

    /**
     * A tick or untick on a day of a finished week updates that week's stored points, so the
     * "Last week" card stays right. Its targets stay as they were at close-out. Call only inside [write].
     */
    private suspend fun refreshClosedWeek(date: LocalDate) {
        val weekStart = WeekMath.weekStart(date)
        if (weekStart >= WeekMath.weekStart(today())) return
        val summary = weekRepository.getSummary(weekStart) ?: return
        val tasks = todayTaskRepository.getBetween(weekStart, weekStart.plus(6, DateTimeUnit.DAY))
        weekRepository.saveSummary(WeekSummaries.withEarned(summary, missionRepository.getAll(), tasks))
    }

    /**
     * Re-picks [date]'s still-planned picks that aren't crowned, moved in or added by hand, and its
     * still-planned fixed blocks too when [withBlocks]. Call only inside [write].
     */
    private suspend fun rebuild(date: LocalDate, plan: DayPlan, withBlocks: Boolean = false) {
        val removed = todayTaskRepository.getBetween(date, date)
            .filter { task ->
                val planned = task.taskState == TaskState.PLANNED
                if (task.isFixedBlock) planned && withBlocks
                else planned && !task.crowned && task.movedFromDate == null && task.reasonCode != ReasonCode.ADDED.name
            }
            .mapTo(mutableSetOf()) { it.todayTaskId }
        fill(date, plan, removed)
    }

    /**
     * Plans [date] as if the items in [removed] were already gone. The picks are worked out first; then
     * [removed] go, the picks come in and the plan row is saved, in one transaction, so a day is never
     * left half planned. Call only inside [write].
     */
    private suspend fun fill(date: LocalDate, plan: DayPlan, removed: Set<Long> = emptySet()) {
        val input = input(date, plan, without = removed)
        // A mission swapped out today stays out of the new picks, but still counts for budget and pillar gaps.
        val result = Planner.plan(input, exclude = swappedOut[date].orEmpty())
        val firstOrder = (input.todayTasks.maxOfOrNull { it.sortOrder } ?: -1) + 1
        planDao.savePicks(
            removed = removed.toList(),
            picks = result.picks.mapIndexed { i, pick -> newTask(pick.mission, date, pick.reason, pick.isFixedBlock, firstOrder + i) },
            plan = plan.copy(
                didntFitMissionId = result.didntFit?.missionId,
                didntFitDue = result.didntFit?.due?.let { key(it) },
                didntFitRoomDay = result.didntFit?.roomDay?.let { key(it) },
            ),
        )
    }

    /** What the planner reads for [date], leaving out the items in [without] (ones about to be re-picked). */
    private suspend fun input(date: LocalDate, plan: DayPlan, without: Set<Long> = emptySet()): PlanInput {
        val missions = missionRepository.getActive()
        val tasks = todayTaskRepository.getBetween(date.minus(HISTORY_DAYS, DateTimeUnit.DAY), date.plus(7, DateTimeUnit.DAY))
            .filter { it.todayTaskId !in without }
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
        val free = days.associateWith { day -> planDao.getPlan(key(day))?.freeMinutes ?: defaultFreeMinutes(day) }
        return Moves.roomByDay(from, minutesById, upcoming, freeOn = { free[it] ?: 0 })
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
