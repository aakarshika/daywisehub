package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
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

    /** Points for doing [mission] in [state]. Recharge never scores, whatever its weight. */
    fun forTask(mission: Mission, state: TaskState): Int =
        if (mission.scored) forState(mission.weight, state) else 0

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
            mission.repeatsByInterval -> {
                // Once per interval: more than once a week for intervals under 7 days, at most once otherwise.
                val due = dueDate(mission, history, weekStart)
                val lastLive = liveDays.last()
                if (due > lastLive) 0
                else 1 + maxOf(due, liveDays.first()).daysUntil(lastLive) / mission.intervalDays()
            }
            mission.periodUnit == Period.DAY -> liveDays.size
            // At most once per live day, so a mission starting mid-week can still be full.
            else -> mission.count.coerceAtMost(liveDays.size)
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

    /** A ring with this many items or more can close by its item count ([closes]). */
    const val CLOSE_MIN_ITEMS = 6
    /** How many of those items can be left undone and the ring still close. */
    const val CLOSE_SPARE = 2

    /**
     * Spec 9.2: a ring that's close enough closes, and is drawn done in its pillar's light color. It closes
     * when it's full, or when it has [CLOSE_MIN_ITEMS] or more [items] and all but [CLOSE_SPARE] are [done].
     * Items are rows of scored missions that didn't move away; done counts did-somes too. With nothing due
     * the ring is drawn full already, so only the item count can close it. It's only a look: points, "full",
     * crowns and pace offers don't change.
     */
    fun closes(earned: Int, target: Int, items: Int, done: Int): Boolean =
        (target > 0 && isFull(earned, target)) || (items >= CLOSE_MIN_ITEMS && done >= items - CLOSE_SPARE)
}
