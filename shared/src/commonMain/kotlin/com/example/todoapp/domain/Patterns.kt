package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Pattern lines on the Metrics tab (spec 9): a few plain counts over stored weeks and ticks. A line
 * shows only when the data holds it up, and nothing reads meaning into a title (spec 7.1).
 */
object Patterns {

    const val MAX_LINES = 3
    /** Full weeks in a row before a pillar gets a line… */
    const val FULL_RUN = 3
    /** …and at most this many pillars get one, so other kinds of line still fit. */
    const val MAX_RUN_LINES = 2
    /** Balanced weeks in a row before they get a line. */
    const val BALANCED_RUN = 2
    /** The weekday line looks at this many weeks, this one included… */
    const val WEEKDAY_WEEKS = 8
    /** …and needs this many ticks in the pillar. */
    const val WEEKDAY_MIN_DONE = 6
    /**
     * Runs look back at most this far. That stays inside MetricsViewModel's year of tasks, which is what
     * tells an away week (no items of its own) from a week that simply wasn't loaded.
     */
    const val MAX_RUN_WEEKS = 51

    private val pluralDays = listOf("Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays")

    /**
     * As of [today], whatever week is on screen. [byWeek] holds the finished weeks by their Monday, and
     * [opened] is PaceOffers.openedWeeks of the tasks read. Ticks of archived missions count too: they happened.
     */
    fun lines(
        today: LocalDate,
        byWeek: Map<LocalDate, WeekSummary>,
        allMissions: List<Mission>,
        days: DayTasks,
        opened: Set<LocalDate>,
    ): List<String> {
        val thisWeek = WeekMath.weekStart(today)
        val lines = mutableListOf<String>()
        val balancedRun = runBack(thisWeek, byWeek, opened) { it.balanced }
        if (balancedRun >= BALANCED_RUN) lines += "$balancedRun balanced weeks in a row."
        // A pillar with nothing due isn't a full week here. A balanced run already covers a shorter pillar run.
        PillarType.scoredPillars
            .map { pillar ->
                pillar to runBack(thisWeek, byWeek, opened) { it.target(pillar) > 0 && Points.isFull(it.earned(pillar), it.target(pillar)) }
            }
            .filter { (_, run) -> run >= FULL_RUN && run > balancedRun }
            .sortedByDescending { it.second }
            .take(MAX_RUN_LINES)
            .forEach { (pillar, run) -> lines += "${pillar.title} has been full $run weeks running." }
        lines += weekdayLines(today, thisWeek, allMissions, days)
        return lines.take(MAX_LINES)
    }

    /**
     * Finished weeks in a row, back from last week, that pass [test]. A week without a row ends the run.
     * An away week (WeekScores.isAway) neither counts nor ends it: its zeros aren't misses.
     */
    private fun runBack(thisWeek: LocalDate, byWeek: Map<LocalDate, WeekSummary>, opened: Set<LocalDate>, test: (WeekSummary) -> Boolean): Int {
        var week = thisWeek.minus(7, DateTimeUnit.DAY)
        var run = 0
        repeat(MAX_RUN_WEEKS) {
            val summary = byWeek[week] ?: return run
            if (!WeekScores.isAway(week, opened)) {
                if (!test(summary)) return run
                run++
            }
            week = week.minus(7, DateTimeUnit.DAY)
        }
        return run
    }

    /**
     * "Love mostly happens on Sundays": the pillar has enough ticks in the last [WEEKDAY_WEEKS] weeks,
     * and one weekday has at least 40% of them and more than any other day. Fixed blocks are left out,
     * since their days are set.
     */
    private fun weekdayLines(today: LocalDate, thisWeek: LocalDate, allMissions: List<Mission>, days: DayTasks): List<String> {
        val byId = allMissions.associateBy { it.missionId }
        val counts = mutableMapOf<PillarType, IntArray>()
        var day = thisWeek.minus(7 * (WEEKDAY_WEEKS - 1), DateTimeUnit.DAY)
        while (day <= today) {
            val weekday = day.dayOfWeek.isoDayNumber - 1
            days.on(day).forEach { task ->
                val mission = byId[task.missionId]
                if (mission != null && mission.scored && !task.isFixedBlock && task.taskState.countsAsDone) {
                    counts.getOrPut(mission.pillarType) { IntArray(7) }[weekday]++
                }
            }
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return PillarType.scoredPillars
            .mapNotNull { pillar ->
                val byDay = counts[pillar] ?: return@mapNotNull null
                val total = byDay.sum()
                val ranked = byDay.sortedArrayDescending()
                val top = ranked[0]
                if (total < WEEKDAY_MIN_DONE || top * 5 < total * 2 || top == ranked[1]) return@mapNotNull null
                Triple(pillar, byDay.indexOf(top), top.toFloat() / total)
            }
            .sortedByDescending { it.third }
            .map { (pillar, weekday, _) -> "${pillar.title} mostly happens on ${pluralDays[weekday]}." }
    }
}
