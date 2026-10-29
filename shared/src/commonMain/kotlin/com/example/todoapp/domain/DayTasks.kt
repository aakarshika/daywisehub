package com.example.todoapp.domain

import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Tasks grouped by day, keyed by their stored yyyy-MM-dd text. The Metrics tab looks days up here,
 * so a month of day cells never reads every task's date again.
 */
class DayTasks(tasks: List<TodayTask>) {

    private val byDay: Map<String, List<TodayTask>> = tasks.groupBy { it.taskDate.dateString }

    fun on(day: LocalDate): List<TodayTask> = byDay[MyDate.fromLocalDate(day).dateString].orEmpty()

    /** Tasks from [from] to [to], both included, in date order. */
    fun between(from: LocalDate, to: LocalDate): List<TodayTask> {
        val out = mutableListOf<TodayTask>()
        var day = from
        while (day <= to) {
            out += on(day)
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return out
    }
}
