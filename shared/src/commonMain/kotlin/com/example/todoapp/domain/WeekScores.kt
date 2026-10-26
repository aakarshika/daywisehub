package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** One pillar's points against its target for one week (spec 5). */
data class PillarScore(val pillar: PillarType, val earned: Int, val target: Int) {
    /** 80% of the target or more; nothing due counts as full. */
    val full: Boolean get() = Points.isFull(earned, target)
    /** Points past the target, shown as "+N extra" on this pillar only. */
    val extra: Int get() = (earned - target).coerceAtLeast(0)
    /** How much of the ring is drawn. A pillar with nothing due counts as full, so its ring is drawn full too. */
    val share: Float get() = if (target == 0) 1f else (earned.toFloat() / target).coerceIn(0f, 1f)
}

/** A week's pillar scores, as the Metrics tab shows them. */
data class WeekScore(
    val pillars: List<PillarScore>,
    /** The crown: stored as WeekSummary.balanced for a finished week, worked out the same way for this one. */
    val balanced: Boolean,
    /** The week is over. */
    val finished: Boolean,
    /**
     * A finished week without a single task: the diary wasn't opened that week. Its stored summary has
     * 0 points against targets, which isn't a miss, so it never counts as a light week.
     */
    val away: Boolean,
)

object WeekScores {

    /**
     * A finished week as stored at close-out, or a week without a row worked out live as the diary's
     * bars are: points from [allMissions] (archived too), targets from the active [missions]. Only this
     * week lacks a row normally; a past week can for a moment, before the diary's close-out runs.
     */
    fun forWeek(
        weekStart: LocalDate,
        thisWeek: LocalDate,
        summary: WeekSummary?,
        missions: List<Mission>,
        allMissions: List<Mission>,
        days: DayTasks,
        history: History,
    ): WeekScore {
        val finished = weekStart < thisWeek
        val away = finished && isAway(weekStart, days)
        if (summary != null) return WeekScore(stored(summary), summary.balanced, finished, away)
        val weekTasks = days.between(weekStart, weekStart.plus(6, DateTimeUnit.DAY))
        val pillars = PillarType.scoredPillars.map { pillar ->
            PillarScore(
                pillar = pillar,
                earned = Points.earnedInWeek(allMissions, weekTasks, pillar, weekStart),
                target = Points.weeklyTarget(missions, pillar, weekStart, history),
            )
        }
        return WeekScore(pillars, balanced(pillars), finished, away)
    }

    fun stored(summary: WeekSummary): List<PillarScore> =
        PillarType.scoredPillars.map { PillarScore(it, summary.earned(it), summary.target(it)) }

    /** WeekSummary.balanced's rule, for a week with no row yet: every pillar full (spec 5), and something was due. */
    fun balanced(pillars: List<PillarScore>): Boolean = pillars.all { it.full } && pillars.any { it.target > 0 }

    /** No task at all in the week starting [weekStart]: the diary wasn't opened in it. */
    fun isAway(weekStart: LocalDate, days: DayTasks): Boolean =
        days.between(weekStart, weekStart.plus(6, DateTimeUnit.DAY)).isEmpty()
}
