package com.example.todoapp.domain

import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

val TodayTask.taskState: TaskState get() = TaskState.valueOf(state)
val TodayTask.date: LocalDate get() = taskDate.toLocalDate()

/** Read-only view over past and upcoming tasks. */
class History(val all: List<TodayTask>) {

    private val byMission: Map<Long, List<TodayTask>> = all.groupBy { it.missionId }

    /** Each mission's tasks in date order, sorted by their stored yyyy-MM-dd text the first time a range is read. */
    private val inOrder: Map<Long, List<TodayTask>> by lazy {
        byMission.mapValues { (_, tasks) -> tasks.sortedBy { it.taskDate.dateString } }
    }

    fun tasksFor(missionId: Long): List<TodayTask> = byMission[missionId].orEmpty()

    /**
     * [missionId]'s tasks from [from] to [to], both included, in date order. Stored yyyy-MM-dd text sorts
     * as dates do, so the range is found by its ends and no task's date is read.
     */
    fun tasksBetween(missionId: Long, from: LocalDate, to: LocalDate): List<TodayTask> {
        val tasks = inOrder[missionId] ?: return emptyList()
        val fromKey = MyDate.fromLocalDate(from).dateString
        val toKey = MyDate.fromLocalDate(to).dateString
        // The first task on or after [from].
        var low = 0
        var high = tasks.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (tasks[mid].taskDate.dateString < fromKey) low = mid + 1 else high = mid
        }
        val out = mutableListOf<TodayTask>()
        while (low < tasks.size && tasks[low].taskDate.dateString <= toKey) out += tasks[low++]
        return out
    }

    fun lastDoneBefore(missionId: Long, before: LocalDate): LocalDate? =
        tasksFor(missionId)
            .filter { it.taskState.countsAsDone && it.date < before }
            .maxOfOrNull { it.date }

    fun doneCount(missionId: Long, from: LocalDate, to: LocalDate): Int =
        tasksFor(missionId).count { it.taskState.countsAsDone && it.date >= from && it.date <= to }

    /** Was it on that day's list (and not moved away or skipped)? */
    fun pickedOn(missionId: Long, date: LocalDate): Boolean =
        tasksFor(missionId).any { it.date == date && it.taskState.isOpen }

    /** Consecutive done days ending the day before [today]. */
    fun streakBefore(missionId: Long, today: LocalDate): Int {
        val doneDays = tasksFor(missionId).filter { it.taskState.countsAsDone }.map { it.date }.toSet()
        var day = today.minus(1, DateTimeUnit.DAY)
        var streak = 0
        while (day in doneDays) {
            streak++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }
}
