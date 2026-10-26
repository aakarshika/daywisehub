package com.example.todoapp.screen.diary

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.History
import com.example.todoapp.domain.Moves
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.Pick
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.ReasonLines
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.WeekScores
import com.example.todoapp.domain.date
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.scored
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.target
import com.example.todoapp.domain.taskState
import com.example.todoapp.domain.weekTitle
import com.example.todoapp.domain.weekdayName
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class PillarProgress(val pillar: PillarType, val earned: Int, val target: Int) {
    /** Bar fill. A pillar with nothing due this week reads full, not empty. */
    val share: Float get() = if (target == 0) 1f else (earned.toFloat() / target).coerceAtMost(1f)
    /** Points past the target, shown as "+N extra" on this pillar only (spec 5). */
    val extra: Int get() = (earned - target).coerceAtLeast(0)
}

data class PlanRow(
    val taskId: Long,
    val title: String,
    val pillar: PillarType,
    val minutes: Int,
    /** The mission's weight; 0 for recharge. */
    val points: Int,
    /** Points this row has earned so far. */
    val pointsEarned: Int,
    val reason: String,
    val state: TaskState,
    val crowned: Boolean,
    val isFixedBlock: Boolean,
    /** Weekday it was moved to, when [state] is MOVED. "next Tuesday" when that is the same weekday a week on. */
    val movedTo: String?,
    /** Moved in from another day; it can't be swapped. */
    val movedIn: Boolean,
    /**
     * Where "Move it" would send it: a weekday, or "today" from an earlier day. Set on planned flexible rows.
     * Null when it would send it nowhere (the mission is live on none of the next 7 days, so it would be
     * skipped) or the page is too old to say; the Move chip is then left out.
     */
    val moveTo: String?,
    /** A fixed block the evening check ticked (spec 6.4: "Ticked for you"). */
    val tickedForYou: Boolean,
    /** MOVED and its copy is still planned: ticking takes the move back (PlanRepository.toggleDone). */
    val canTakeBack: Boolean,
)

data class DidntFitRow(val title: String, val text: String)

/** One mission in the pencil's list, with its reason line. */
data class AddOption(val missionId: Long, val title: String, val pillar: PillarType, val minutes: Int, val reason: String) {
    companion object {
        fun from(pick: Pick) = AddOption(
            missionId = pick.mission.missionId,
            title = pick.mission.title,
            pillar = pick.mission.pillarType,
            minutes = pick.mission.durationMinutes,
            reason = ReasonLines.render(pick.reason),
        )
    }
}

data class DiaryUiState(
    val pillars: List<PillarProgress>,
    val fixed: List<PlanRow>,
    val picks: List<PlanRow>,
    val didntFit: DidntFitRow?,
    val hasPlan: Boolean,
    val freeMinutes: Int,
    val energy: Energy,
    val plannedMinutes: Int,
    val confirmed: Boolean,
    val pointsToday: Int,
    val pillarsTouched: Int,
    val crownedDone: Int,
    val crownedTotal: Int,
    val todayMissionIds: Set<Long>,
    /** Picks done or did-some. */
    val donePicks: Int,
    /** False on an empty install: the list then points to the Missions tab. */
    val hasMissions: Boolean,
    /** Shown on the day that opens the week (WeekStateBuilder.opensWeek). */
    val lastWeek: LastWeekState?,
    /** Pace offers, on today's page when it opens the week. */
    val offers: List<OfferNote>,
    /**
     * The strip's label for this day's week: "THIS WEEK", "LAST WEEK", "NEXT WEEK" or "WEEK OF OCT 12"
     * (with the year in another year than this week's), as the Metrics tab names weeks.
     */
    val weekLabel: String,
    /** Today's week has just begun: the strip reads "THIS WEEK · A FRESH START". Only on today's page. */
    val freshStart: Boolean,
) {
    /** Every open scored pick resolved as done or did-some; drives the confetti. */
    val allPicksDone: Boolean
        get() = picks.filter { it.state.isOpen && it.pillar.scored }.let { open -> open.isNotEmpty() && open.all { it.state.countsAsDone } }
}

object DiaryStateBuilder {

    /**
     * [missions] are the active ones; everything is built from them except points earned, which use
     * [allMissions] (archived too) so a mission archived mid-week keeps the points it already earned.
     */
    fun build(
        date: LocalDate,
        today: LocalDate,
        missions: List<Mission>,
        allMissions: List<Mission>,
        tasks: List<TodayTask>,
        extras: DiaryExtras,
    ): DiaryUiState {
        val dateKey = MyDate.fromLocalDate(date).dateString
        val plan = extras.plans.firstOrNull { it.date == dateKey }
        val history = History(tasks)
        val opensWeek = WeekStateBuilder.opensWeek(date, extras.plans)
        val weekStart = WeekMath.weekStart(date)
        val thisWeek = WeekMath.weekStart(today)
        // The adjusted lines read today's missions, so only today's page says them. Nor is an earlier page a fresh start.
        val lastWeek = if (opensWeek) {
            // Away by the Metrics tab's rule, from the tasks the diary already reads; they start months before last week.
            val away = WeekScores.isAway(weekStart.minus(7, DateTimeUnit.DAY), PaceOffers.openedWeeks(tasks))
            WeekStateBuilder.lastWeek(extras.lastWeek, away, missions, history, withAdjusted = date == today)
        } else null
        val byId = missions.associateBy { it.missionId }
        // A finished week's targets are the ones stored at its close-out, as on the Metrics tab. Its points
        // stay live, so a tick on an earlier day shows at once.
        val stored = extras.pageWeek?.takeIf { weekStart < thisWeek }
        val pillars = PillarType.scoredPillars.map { pillar ->
            PillarProgress(
                pillar = pillar,
                earned = Points.earnedInWeek(allMissions, tasks, pillar, weekStart),
                target = stored?.target(pillar) ?: Points.weeklyTarget(missions, pillar, weekStart, history),
            )
        }
        val todayTasks = tasks.filter { it.date == date }.sortedWith(compareBy({ it.sortOrder }, { it.todayTaskId }))
        // "Move it" names its day, found the way PlanRepository.moveToNextRoom finds it. The 7 days it looks at
        // are sliced out once, and only when a planned flexible row needs them.
        val anchor = Moves.anchor(date, today)
        val naming = namesMoves(date, today) && todayTasks.any { it.taskState == TaskState.PLANNED && !it.isFixedBlock }
        val ahead = if (naming) {
            val last = anchor.plus(7, DateTimeUnit.DAY)
            tasks.filter { task -> task.date.let { it > anchor && it <= last } }
        } else emptyList()
        val room = if (naming) {
            Moves.roomByDay(anchor, missions.associate { it.missionId to it.durationMinutes }, ahead) { day -> freeOn(day, extras) }
        } else emptyMap()
        val rows = todayTasks.mapNotNull { task ->
            val mission = byId[task.missionId] ?: return@mapNotNull null
            val moveTo = if (naming && task.taskState == TaskState.PLANNED && !task.isFixedBlock) {
                Moves.target(mission, ahead, room)?.let { if (it == today) "today" else it.dayWord(date) }
            } else null
            toRow(task, mission, history, moveTo)
        }
        val open = rows.filter { it.state.isOpen }
        val crowned = rows.filter { it.crowned && it.state.isOpen }
        val picks = rows.filter { !it.isFixedBlock }
        return DiaryUiState(
            pillars = pillars,
            fixed = rows.filter { it.isFixedBlock },
            picks = picks,
            didntFit = didntFit(date, plan, byId, todayTasks),
            hasPlan = plan != null,
            freeMinutes = plan?.freeMinutes ?: 0,
            energy = plan?.let { Energy.valueOf(it.energy) } ?: Energy.NORMAL,
            plannedMinutes = open.filter { !it.isFixedBlock }.sumOf { it.minutes },
            confirmed = plan?.confirmed ?: true,
            pointsToday = todayTasks.sumOf { it.pointsEarned },
            pillarsTouched = rows.filter { it.state.countsAsDone && it.pillar.scored }.map { it.pillar }.distinct().size,
            crownedDone = crowned.count { it.state.countsAsDone },
            crownedTotal = crowned.size,
            todayMissionIds = todayTasks.map { it.missionId }.toSet(),
            donePicks = picks.count { it.state.countsAsDone },
            hasMissions = missions.isNotEmpty(),
            lastWeek = lastWeek,
            offers = if (date == today && opensWeek) WeekStateBuilder.offers(extras.offers, missions, today) else emptyList(),
            weekLabel = weekTitle(weekStart, thisWeek).uppercase(),
            freshStart = date == today && lastWeek != null && pillars.all { it.earned == 0 },
        )
    }

    /**
     * Can [date]'s page name where "Move it" goes? From yesterday on, yes. An older page's days to move
     * to are past what the diary reads (DiaryViewModel), and its planned items were settled long ago.
     */
    fun namesMoves(date: LocalDate, today: LocalDate): Boolean = Moves.anchor(date, today) == date

    /** Free minutes on [day]: its plan's if it has one, else its weekday's setting, as in PlanRepository. */
    private fun freeOn(day: LocalDate, extras: DiaryExtras): Int {
        val key = MyDate.fromLocalDate(day).dateString
        extras.plans.firstOrNull { it.date == key }?.let { return it.freeMinutes }
        val weekday = day.dayOfWeek.isoDayNumber
        return extras.settings.firstOrNull { it.weekday == weekday }?.freeMinutes ?: DaySettings.defaultFor(weekday)
    }

    private fun toRow(task: TodayTask, mission: Mission, history: History, moveTo: String?): PlanRow {
        val moved = task.taskState == TaskState.MOVED
        val copy = if (moved) Moves.copyOf(task, history.tasksFor(task.missionId)) else null
        return PlanRow(
            taskId = task.todayTaskId,
            title = mission.title,
            pillar = mission.pillarType,
            minutes = mission.durationMinutes,
            points = if (mission.scored) mission.weight else 0,
            pointsEarned = task.pointsEarned,
            reason = ReasonLines.render(task.reasonCode, task.reasonArgs, on = task.date),
            state = task.taskState,
            crowned = task.crowned,
            isFixedBlock = task.isFixedBlock,
            movedTo = copy?.date?.dayWord(task.date),
            movedIn = task.movedFromDate != null,
            moveTo = moveTo,
            tickedForYou = task.isFixedBlock && task.taskState == TaskState.DONE && task.autoTicked,
            canTakeBack = Moves.canTakeBack(task, copy),
        )
    }

    /** "Thursday", or "next Thursday" when it is the same weekday as [from], so a week on isn't read as [from] itself. */
    private fun LocalDate.dayWord(from: LocalDate): String =
        if (dayOfWeek == from.dayOfWeek) "next ${weekdayName()}" else weekdayName()

    private fun didntFit(date: LocalDate, plan: DayPlan?, byId: Map<Long, Mission>, todayTasks: List<TodayTask>): DidntFitRow? {
        val missionId = plan?.didntFitMissionId ?: return null
        if (todayTasks.any { it.missionId == missionId }) return null
        val mission = byId[missionId] ?: return null
        val due = plan.didntFitDue?.let { MyDate(it).toLocalDate() } ?: return null
        val room = plan.didntFitRoomDay?.let { MyDate(it).toLocalDate() }
        val dueText = if (due < date) "Overdue since ${due.shortLabel()}." else "Due by ${due.shortLabel()}."
        val text = dueText + (room?.let { " ${it.weekdayName()} has room." } ?: "")
        return DidntFitRow(mission.title, text)
    }
}
