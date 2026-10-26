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
    /** …and needs this many ticks in the pillar, more than half of them on one weekday. */
    const val WEEKDAY_MIN_DONE = 8
    /**
     * How far back the tab's as-of-today lines look: patterns, and the mission lines' counts, best streaks
     * and average gaps. MetricsViewModel always reads at least this much, so browsing an older month
     * doesn't change them.
     */
    const val LOOK_BACK_DAYS = 365
    /**
     * Runs look back at most this many weeks before this one, inside [LOOK_BACK_DAYS]. That's what tells an
     * away week (no items of its own) from a week that simply wasn't loaded.
     */
    const val MAX_RUN_WEEKS = LOOK_BACK_DAYS / 7 - 1

    private val pluralDays = listOf("Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays")

    /**
     * A run of weeks back from last week. [capped] when it reached [MAX_RUN_WEEKS] without ending: it then
     * reads "for a year or more". The mission lines' "full weeks in a row" uses it too.
     */
    data class Run(val weeks: Int, val capped: Boolean)

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
        val runs = mutableListOf<String>()
        val balanced = runBack(thisWeek, byWeek, opened) { it.balanced }
        if (balanced.weeks >= BALANCED_RUN) {
            runs += if (balanced.capped) "Balanced weeks in a row for a year or more." else "${balanced.weeks} balanced weeks in a row."
        }
        // A pillar with nothing due isn't a full week here. A balanced run already covers a shorter pillar run.
        PillarType.scoredPillars
            .map { pillar ->
                pillar to runBack(thisWeek, byWeek, opened) { it.target(pillar) > 0 && Points.isFull(it.earned(pillar), it.target(pillar)) }
            }
            .filter { (_, run) -> run.weeks >= FULL_RUN && run.weeks > balanced.weeks }
            .sortedByDescending { it.second.weeks }
            .take(MAX_RUN_LINES)
            .forEach { (pillar, run) ->
                runs += if (run.capped) "${pillar.title} has been full for a year or more." else "${pillar.title} has been full ${run.weeks} weeks running."
            }
        // A weekday line keeps a slot, so run lines can't crowd it out.
        val weekdays = weekdayLines(today, thisWeek, allMissions, days)
        return (runs.take(MAX_LINES - minOf(weekdays.size, 1)) + weekdays).take(MAX_LINES)
    }

    /**
     * Finished weeks in a row, back from last week, that pass [test]. A week without a row ends the run.
     * An away week (WeekScores.isAway) neither counts nor ends it: its zeros aren't misses.
     */
    private fun runBack(thisWeek: LocalDate, byWeek: Map<LocalDate, WeekSummary>, opened: Set<LocalDate>, test: (WeekSummary) -> Boolean): Run {
        var week = thisWeek.minus(7, DateTimeUnit.DAY)
        var run = 0
        repeat(MAX_RUN_WEEKS) {
            val summary = byWeek[week] ?: return Run(run, capped = false)
            if (!WeekScores.isAway(week, opened)) {
                if (!test(summary)) return Run(run, capped = false)
                run++
            }
            week = week.minus(7, DateTimeUnit.DAY)
        }
        return Run(run, capped = true)
    }

    /**
     * "Love mostly happens on Mondays": in the last [WEEKDAY_WEEKS] weeks the pillar has at least
     * [WEEKDAY_MIN_DONE] ticks, and more than half of them fall on one weekday, so "mostly" holds and no
     * other day ties it. Only missions free to land on any day count: fixed blocks, daily missions and
     * missions with preferred days are left out, since their days are set.
     */
    private fun weekdayLines(today: LocalDate, thisWeek: LocalDate, allMissions: List<Mission>, days: DayTasks): List<String> {
        val byId = allMissions.associateBy { it.missionId }
        val counts = mutableMapOf<PillarType, IntArray>()
        var day = thisWeek.minus(7 * (WEEKDAY_WEEKS - 1), DateTimeUnit.DAY)
        while (day <= today) {
            val weekday = day.dayOfWeek.isoDayNumber - 1
            days.on(day).forEach { task ->
                val mission = byId[task.missionId]
                if (mission != null && mission.scored && freeDay(mission) && !task.isFixedBlock && task.taskState.countsAsDone) {
                    counts.getOrPut(mission.pillarType) { IntArray(7) }[weekday]++
                }
            }
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return PillarType.scoredPillars
            .mapNotNull { pillar ->
                val byDay = counts[pillar] ?: return@mapNotNull null
                val total = byDay.sum()
                val top = byDay.max()
                if (total < WEEKDAY_MIN_DONE || top * 2 <= total) return@mapNotNull null
                Triple(pillar, byDay.indexOf(top), top.toFloat() / total)
            }
            .sortedByDescending { it.third }
            .map { (pillar, weekday, _) -> "${pillar.title} mostly happens on ${pluralDays[weekday]}." }
    }

    /** Not a fixed block, not every day, and no preferred days: which weekday it lands on is up to you. */
    private fun freeDay(mission: Mission): Boolean =
        mission.missionKind != MissionKind.BLOCK && mission.periodUnit != Period.DAY && mission.daySet.isEmpty()
}
