package com.example.todoapp.screen.diary

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.History
import com.example.todoapp.domain.Moves
import com.example.todoapp.domain.Pick
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.ReasonLines
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.date
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.scored
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.taskState
import com.example.todoapp.domain.weekdayName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

data class PillarProgress(val pillar: PillarType, val earned: Int, val target: Int) {
    val share: Float get() = if (target == 0) 0f else (earned.toFloat() / target).coerceAtMost(1f)
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
    /** Weekday it was moved to, when [state] is MOVED. */
    val movedTo: String?,
    /** Moved in from another day; it can't be swapped. */
    val movedIn: Boolean,
    /** Where "Move it" would send it: a weekday, or "today" from an earlier day. Set on planned flexible rows. */
    val moveTo: String?,
    /** A fixed block the evening check ticked (spec 6.4: "Ticked for you"). */
    val tickedForYou: Boolean,
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
    /** The week has just begun: the strip reads "THIS WEEK · A FRESH START". */
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
        val plan = extras.plans.firstOrNull { it.date == MyDate.fromLocalDate(date).dateString }
        val opensWeek = WeekStateBuilder.opensWeek(date, extras.plans)
        val lastWeek = if (opensWeek) WeekStateBuilder.lastWeek(extras.lastWeek, missions, tasks) else null
        val byId = missions.associateBy { it.missionId }
        val history = History(tasks)
        val weekStart = WeekMath.weekStart(date)
        val pillars = PillarType.scoredPillars.map { pillar ->
            PillarProgress(
                pillar = pillar,
                earned = Points.earnedInWeek(allMissions, tasks, pillar, weekStart),
                target = Points.weeklyTarget(missions, pillar, weekStart, history),
            )
        }
        val todayTasks = tasks.filter { it.date == date }.sortedWith(compareBy({ it.sortOrder }, { it.todayTaskId }))
        // "Move it" names its day, found the way PlanRepository.moveToNextRoom finds it.
        val after = Moves.anchor(date, today)
        val room = Moves.roomByDay(after, missions.associate { it.missionId to it.durationMinutes }, tasks) { day -> freeOn(day, extras) }
        val rows = todayTasks.mapNotNull { task ->
            val mission = byId[task.missionId] ?: return@mapNotNull null
            val moveTo = if (task.taskState == TaskState.PLANNED && !task.isFixedBlock) {
                Moves.target(mission, tasks, room)?.let { if (it == today) "today" else it.weekdayName() }
            } else null
            val ticked = task.isFixedBlock && task.taskState == TaskState.DONE && plan?.fixedTicked == true
            toRow(task, mission, tasks, moveTo, ticked)
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
            freshStart = lastWeek != null && pillars.all { it.earned == 0 },
        )
    }

    /** Free minutes on [day]: its plan's if it has one, else its weekday's setting, as in PlanRepository. */
    private fun freeOn(day: LocalDate, extras: DiaryExtras): Int {
        val key = MyDate.fromLocalDate(day).dateString
        extras.plans.firstOrNull { it.date == key }?.let { return it.freeMinutes }
        val weekday = day.dayOfWeek.isoDayNumber
        return extras.settings.firstOrNull { it.weekday == weekday }?.freeMinutes ?: DaySettings.defaultFor(weekday)
    }

    private fun toRow(task: TodayTask, mission: Mission, tasks: List<TodayTask>, moveTo: String?, tickedForYou: Boolean): PlanRow {
        val movedTo = if (task.taskState == TaskState.MOVED) {
            tasks.firstOrNull { it.missionId == task.missionId && it.movedFromDate == task.taskDate }?.date?.weekdayName()
        } else null
        return PlanRow(
            taskId = task.todayTaskId,
            title = mission.title,
            pillar = mission.pillarType,
            minutes = mission.durationMinutes,
            points = if (mission.scored) mission.weight else 0,
            pointsEarned = task.pointsEarned,
            reason = ReasonLines.render(task.reasonCode, task.reasonArgs),
            state = task.taskState,
            crowned = task.crowned,
            isFixedBlock = task.isFixedBlock,
            movedTo = movedTo,
            movedIn = task.movedFromDate != null,
            moveTo = moveTo,
            tickedForYou = tickedForYou,
        )
    }

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
