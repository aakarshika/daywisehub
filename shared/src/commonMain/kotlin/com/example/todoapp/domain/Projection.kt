package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * The days after today, through the end of next week, get a projected list: the planner's guess, made as if
 * each day before goes to plan. It's re-picked as days go by and missions change. A projected day's plain
 * picks are only a guess, so moves and "one catch-up is enough" look past them, and a projected day that
 * goes by without the diary opening on it loses them, as if it was never planned.
 */
object Projection {

    /** The last projected day: the Sunday of next week. */
    fun lastDay(today: LocalDate): LocalDate = WeekMath.weekStart(today).plus(13, DateTimeUnit.DAY)

    /** A pick the planner may redo: still planned, not crowned, not moved in and not added by hand. */
    fun repickable(task: TodayTask): Boolean =
        task.taskState == TaskState.PLANNED && !task.crowned && task.movedFromDate == null &&
            task.reasonCode != ReasonCode.ADDED.name

    /** [tasks] without the repickable picks on [projectedDays] (yyyy-MM-dd). */
    fun kept(tasks: List<TodayTask>, projectedDays: Set<String>): List<TodayTask> =
        if (projectedDays.isEmpty()) tasks
        else tasks.filterNot { it.taskDate.dateString in projectedDays && repickable(it) }

    /** What the planner reads when projecting [date]: items still planned from [today] to the day before count as done. */
    fun assumingDone(tasks: List<TodayTask>, missions: List<Mission>, today: LocalDate, date: LocalDate): List<TodayTask> {
        val byId = missions.associateBy { it.missionId }
        return tasks.map { task ->
            val mission = byId[task.missionId]
            if (mission != null && task.taskState == TaskState.PLANNED && task.date >= today && task.date < date) {
                task.copy(state = TaskState.DONE.name, pointsEarned = Points.forTask(mission, TaskState.DONE))
            } else task
        }
    }
}
