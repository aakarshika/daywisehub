package com.example.todoapp.screen.metrics

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.domain.AheadLine
import com.example.todoapp.domain.DayTasks
import com.example.todoapp.domain.History
import com.example.todoapp.domain.MissionLine
import com.example.todoapp.domain.MissionProgress
import com.example.todoapp.domain.MonthCalendar
import com.example.todoapp.domain.MonthGrid
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.Patterns
import com.example.todoapp.domain.WeekAhead
import com.example.todoapp.domain.WeekHighlights
import com.example.todoapp.domain.WeekHighlightsBuilder
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.WeekScore
import com.example.todoapp.domain.WeekScores
import com.example.todoapp.domain.labelFrom
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.weekStartDate
import com.example.todoapp.domain.weekTitle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
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
    /** "This week", "Last week" or "Week of Oct 12" ("Week of Dec 22, 2025" in another year than this week's). */
    val title: String,
    /** "Oct 26 to Nov 1", with the year on a date in another year than this week's. */
    val range: String,
    val isThisWeek: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    /** The week's pillar scores: stored for a finished week, live for this one. */
    val score: WeekScore,
    val highlights: WeekHighlights,
    /** "NEXT WEEK", or "THE WEEK AFTER" for a finished week. */
    val aheadTitle: String,
    /** What next week adjusts, each line naming its mission on a chip. Never empty. */
    val ahead: List<AheadLine>,
    /** As of today, whichever week is on screen. Empty until the data holds a pattern up. */
    val patterns: List<String>,
    /** The month of the week on screen. */
    val calendar: MonthCalendar,
    /** One per active mission, as of today, in pillar order. */
    val missions: List<MissionLine>,
)

object MetricsStateBuilder {

    fun build(input: MetricsInput): MetricsUiState {
        val week = input.week
        val thisWeek = WeekMath.weekStart(input.today)
        // Read once here; every section below looks days and weeks up instead of re-reading dates.
        val missions = input.allMissions.filter { it.active }
        val days = DayTasks(input.tasks)
        val history = History(input.tasks)
        val byWeek = input.summaries.associateBy { it.weekStartDate }
        // The weeks the diary was opened in, once for every section: the rest of the finished weeks are away.
        val opened = PaceOffers.openedWeeks(input.tasks)
        val score = WeekScores.forWeek(week, thisWeek, byWeek[week], missions, input.allMissions, days, history, opened)
        val title = weekTitle(week, thisWeek)
        val end = week.plus(6, DateTimeUnit.DAY)
        return MetricsUiState(
            weekStart = week,
            title = title,
            // The year goes once, on the end, unless the week spans two years.
            range = "${if (week.year == end.year) week.shortLabel() else week.labelFrom(thisWeek)} to ${end.labelFrom(thisWeek)}",
            isThisWeek = week == thisWeek,
            canGoBack = week > earliest(input.summaries, thisWeek),
            canGoForward = week < thisWeek,
            score = score,
            highlights = WeekHighlightsBuilder.build(title.uppercase(), week, thisWeek, score, byWeek, opened),
            aheadTitle = if (week < thisWeek) "THE WEEK AFTER" else "NEXT WEEK",
            ahead = WeekAhead.lines(week, input.today, missions, history),
            patterns = Patterns.lines(input.today, byWeek, input.allMissions, days, opened),
            calendar = MonthGrid.build(week, input.today, input.allMissions, days, byWeek, opened),
            missions = MissionProgress.lines(input.today, missions, history, input.milestones, opened),
        )
    }

    /** The left arrow stops here. Stored weeks start at the week of the first task, so nothing comes before the first one. */
    fun earliest(summaries: List<WeekSummary>, thisWeek: LocalDate): LocalDate =
        summaries.firstOrNull()?.weekStartDate?.let { minOf(it, thisWeek) } ?: thisWeek

}
