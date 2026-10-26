package com.example.todoapp.screen.metrics

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.weekStartDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Everything the Metrics tab reads. */
data class MetricsInput(
    val today: LocalDate,
    /** Monday of the week on screen. */
    val week: LocalDate,
    /** Active and archived missions. Archived ones still count for the points they earned. */
    val allMissions: List<Mission>,
    /** Tasks in MetricsViewModel's window: a year back from today, the shown week's month and its history, and next week. */
    val tasks: List<TodayTask>,
    /** Every finished week, oldest first. */
    val summaries: List<WeekSummary>,
    /** Milestones of active missions. */
    val milestones: List<Milestone>,
)

data class MetricsUiState(
    val weekStart: LocalDate,
    /** "This week", "Last week" or "Week of Oct 12". */
    val title: String,
    /** "Oct 26 to Nov 1" */
    val range: String,
    val isThisWeek: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
)

object MetricsStateBuilder {

    fun build(input: MetricsInput): MetricsUiState {
        val week = input.week
        val thisWeek = WeekMath.weekStart(input.today)
        // Stored weeks start at the week of the first task, so nothing comes before the first one.
        val earliest = input.summaries.firstOrNull()?.weekStartDate?.let { minOf(it, thisWeek) } ?: thisWeek
        return MetricsUiState(
            weekStart = week,
            title = title(week, thisWeek),
            range = "${week.shortLabel()} to ${week.plus(6, DateTimeUnit.DAY).shortLabel()}",
            isThisWeek = week == thisWeek,
            canGoBack = week > earliest,
            canGoForward = week < thisWeek,
        )
    }

    private fun title(week: LocalDate, thisWeek: LocalDate): String = when (week) {
        thisWeek -> "This week"
        thisWeek.minus(7, DateTimeUnit.DAY) -> "Last week"
        else -> "Week of ${week.shortLabel()}"
    }
}
