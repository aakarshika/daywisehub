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
     * A finished week without an item of its own: the diary wasn't opened that week. Its stored summary
     * has 0 points against targets, which isn't a miss, so it shows no numbers and never counts as light.
     */
    val away: Boolean,
)

object WeekScores {

    /**
     * A finished week as stored at close-out, or a week without a row worked out live as the diary's
     * bars are: points from [allMissions] (archived too), targets from the active [missions]. Only this
     * week lacks a row normally; a past week can for a moment, before the diary's close-out runs.
     * [opened] is PaceOffers.openedWeeks of the same tasks.
     */
    fun forWeek(
        weekStart: LocalDate,
        thisWeek: LocalDate,
        summary: WeekSummary?,
        missions: List<Mission>,
        allMissions: List<Mission>,
        days: DayTasks,
        history: History,
        opened: Set<LocalDate>,
    ): WeekScore {
        val finished = weekStart < thisWeek
        val away = finished && isAway(weekStart, opened)
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

    /** WeekSummary.balanced's rule (WeekSummaries.balanced), for a week with no row yet. */
    fun balanced(pillars: List<PillarScore>): Boolean {
        val byPillar = pillars.associateBy { it.pillar }
        return WeekSummaries.balanced({ byPillar[it]?.earned ?: 0 }, { byPillar[it]?.target ?: 0 })
    }

    /**
     * No item of its own in the finished week starting [weekStart] (items moved in don't count): the diary
     * wasn't opened in it. [opened] is PaceOffers.openedWeeks of the tasks read, worked out once per build.
     */
    fun isAway(weekStart: LocalDate, opened: Set<LocalDate>): Boolean = weekStart !in opened
}
