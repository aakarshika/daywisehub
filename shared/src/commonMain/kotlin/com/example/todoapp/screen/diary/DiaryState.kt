package com.example.todoapp.screen.diary

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.History
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.ReasonLines
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.date
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.taskState
import com.example.todoapp.domain.weekdayName
import kotlinx.datetime.LocalDate

data class PillarProgress(val pillar: PillarType, val earned: Int, val target: Int) {
    val share: Float get() = if (target == 0) 0f else (earned.toFloat() / target).coerceAtMost(1f)
}

data class PlanRow(
    val taskId: Long,
    val title: String,
    val pillar: PillarType,
    val minutes: Int,
    /** The mission's weight; 0 for recharge. */
    val points: Int,
    val reason: String,
    val state: TaskState,
    val crowned: Boolean,
    val isFixedBlock: Boolean,
    /** Weekday it was moved to, when [state] is MOVED. */
    val movedTo: String?,
)

data class DidntFitRow(val title: String, val text: String)

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
) {
    /** Every open pick resolved as done or did-some; drives the confetti. */
    val allPicksDone: Boolean
        get() = picks.any { it.state.isOpen } && picks.filter { it.state.isOpen }.all { it.state.countsAsDone }
}

object DiaryStateBuilder {

    fun build(date: LocalDate, missions: List<Mission>, tasks: List<TodayTask>, plan: DayPlan?): DiaryUiState {
        val byId = missions.associateBy { it.missionId }
        val history = History(tasks)
        val weekStart = WeekMath.weekStart(date)
        val pillars = PillarType.scoredPillars.map { pillar ->
            PillarProgress(
                pillar = pillar,
                earned = Points.earnedInWeek(missions, tasks, pillar, weekStart),
                target = Points.weeklyTarget(missions, pillar, weekStart, history),
            )
        }
        val todayTasks = tasks.filter { it.date == date }.sortedBy { it.sortOrder }
        val rows = todayTasks.mapNotNull { task -> byId[task.missionId]?.let { toRow(task, it, tasks) } }
        val open = rows.filter { it.state.isOpen }
        val crowned = rows.filter { it.crowned }
        return DiaryUiState(
            pillars = pillars,
            fixed = rows.filter { it.isFixedBlock },
            picks = rows.filter { !it.isFixedBlock },
            didntFit = didntFit(plan, byId, todayTasks),
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
        )
    }

    private fun toRow(task: TodayTask, mission: Mission, tasks: List<TodayTask>): PlanRow {
        val movedTo = if (task.taskState == TaskState.MOVED) {
            tasks.firstOrNull { it.missionId == task.missionId && it.movedFromDate == task.taskDate }?.date?.weekdayName()
        } else null
        return PlanRow(
            taskId = task.todayTaskId,
            title = mission.title,
            pillar = mission.pillarType,
            minutes = mission.durationMinutes,
            points = mission.weight,
            reason = ReasonLines.render(task.reasonCode, task.reasonArgs),
            state = task.taskState,
            crowned = task.crowned,
            isFixedBlock = task.isFixedBlock,
            movedTo = movedTo,
        )
    }

    private fun didntFit(plan: DayPlan?, byId: Map<Long, Mission>, todayTasks: List<TodayTask>): DidntFitRow? {
        val missionId = plan?.didntFitMissionId ?: return null
        if (todayTasks.any { it.missionId == missionId }) return null
        val mission = byId[missionId] ?: return null
        val due = plan.didntFitDue?.let { MyDate(it).toLocalDate() } ?: return null
        val room = plan.didntFitRoomDay?.let { MyDate(it).toLocalDate() }
        val text = "Due by ${due.shortLabel()}." + (room?.let { " ${it.weekdayName()} has room." } ?: "")
        return DidntFitRow(mission.title, text)
    }
}
