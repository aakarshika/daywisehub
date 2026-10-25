package com.example.todoapp.domain

import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

val TodayTask.taskState: TaskState get() = TaskState.valueOf(state)
val TodayTask.date: LocalDate get() = taskDate.toLocalDate()

/** Read-only view over past and upcoming tasks. */
class History(val all: List<TodayTask>) {

    private val byMission: Map<Long, List<TodayTask>> = all.groupBy { it.missionId }

    fun tasksFor(missionId: Long): List<TodayTask> = byMission[missionId].orEmpty()

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
