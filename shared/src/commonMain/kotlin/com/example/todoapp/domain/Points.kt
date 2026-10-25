package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Spec section 5. */
object Points {

    /** A pillar counts as full at 80% of its target. */
    const val FULL_SHARE = 0.8

    fun forState(weight: Int, state: TaskState): Int = when (state) {
        TaskState.DONE -> weight
        TaskState.PARTIAL -> (weight + 1) / 2
        else -> 0
    }

    /**
     * Due date of an interval mission, judged only from what was done before [weekStart],
     * so the week's target doesn't change when it gets done mid-week.
     */
    fun dueDate(mission: Mission, history: History, weekStart: LocalDate): LocalDate {
        val last = history.lastDoneBefore(mission.missionId, weekStart)
        return last?.plus(mission.intervalDays(), DateTimeUnit.DAY) ?: mission.startDate.toLocalDate()
    }

    /** How many times the mission is due in the week starting [weekStart]. */
    fun occurrencesInWeek(mission: Mission, weekStart: LocalDate, history: History): Int {
        val days = (0..6).map { weekStart.plus(it, DateTimeUnit.DAY) }
        val liveDays = days.filter { mission.isLiveOn(it) }
        if (liveDays.isEmpty()) return 0
        return when {
            mission.missionKind == MissionKind.BLOCK -> liveDays.count { mission.isScheduledOn(it) }
            mission.repeatsByInterval -> if (dueDate(mission, history, weekStart) <= days.last()) 1 else 0
            mission.periodUnit == Period.DAY -> liveDays.size
            else -> mission.count
        }
    }

    fun weeklyTarget(missions: List<Mission>, pillar: PillarType, weekStart: LocalDate, history: History): Int =
        missions
            .filter { it.pillarType == pillar && it.scored }
            .sumOf { it.weight * occurrencesInWeek(it, weekStart, history) }

    fun earnedInWeek(missions: List<Mission>, tasks: List<TodayTask>, pillar: PillarType, weekStart: LocalDate): Int {
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
        val ids = missions.filter { it.pillarType == pillar }.map { it.missionId }.toSet()
        return tasks
            .filter { it.missionId in ids && it.date >= weekStart && it.date <= weekEnd }
            .sumOf { it.pointsEarned }
    }

    fun isFull(earned: Int, target: Int): Boolean = target == 0 || earned >= target * FULL_SHARE
}
