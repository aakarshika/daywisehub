package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.LocalDate

val WeekSummary.weekStartDate: LocalDate get() = MyDate(weekStart).toLocalDate()

fun WeekSummary.earned(pillar: PillarType): Int = when (pillar) {
    PillarType.HEALTH -> healthEarned
    PillarType.WEALTH -> wealthEarned
    PillarType.LOVE -> loveEarned
    PillarType.LIFE -> lifeEarned
    PillarType.RECHARGE -> 0
}

fun WeekSummary.target(pillar: PillarType): Int = when (pillar) {
    PillarType.HEALTH -> healthTarget
    PillarType.WEALTH -> wealthTarget
    PillarType.LOVE -> loveTarget
    PillarType.LIFE -> lifeTarget
    PillarType.RECHARGE -> 0
}

/** Pillars at 80% of their target or more; a pillar with nothing due counts as full (spec 5). */
fun WeekSummary.pillarsFull(): Int = PillarType.scoredPillars.count { Points.isFull(earned(it), target(it)) }

/**
 * Spec 5.1. Nothing carries over between weeks: earned points are the week's own tasks, and each
 * week's target comes from the missions' paces, never from what was missed the week before.
 */
object WeekSummaries {

    /**
     * The finished week starting [weekStart]. Targets come from the active [missions] as they stand at
     * close-out; points from the week's tasks of [allMissions], archived too, as in the diary's bars.
     */
    fun summarize(weekStart: LocalDate, missions: List<Mission>, allMissions: List<Mission>, history: History): WeekSummary {
        val earned = PillarType.scoredPillars.associateWith { Points.earnedInWeek(allMissions, history.all, it, weekStart) }
        val target = PillarType.scoredPillars.associateWith { Points.weeklyTarget(missions, it, weekStart, history) }
        fun e(pillar: PillarType) = earned.getValue(pillar)
        fun t(pillar: PillarType) = target.getValue(pillar)
        return WeekSummary(
            weekStart = MyDate.fromLocalDate(weekStart).dateString,
            healthEarned = e(PillarType.HEALTH),
            healthTarget = t(PillarType.HEALTH),
            wealthEarned = e(PillarType.WEALTH),
            wealthTarget = t(PillarType.WEALTH),
            loveEarned = e(PillarType.LOVE),
            loveTarget = t(PillarType.LOVE),
            lifeEarned = e(PillarType.LIFE),
            lifeTarget = t(PillarType.LIFE),
            balanced = PillarType.scoredPillars.all { Points.isFull(e(it), t(it)) },
        )
    }

    /**
     * [summary] with its points added up again from [tasks] of [allMissions] (after a tick on one of its
     * days). The targets stay as stored.
     */
    fun withEarned(summary: WeekSummary, allMissions: List<Mission>, tasks: List<TodayTask>): WeekSummary {
        val start = summary.weekStartDate
        fun e(pillar: PillarType) = Points.earnedInWeek(allMissions, tasks, pillar, start)
        val updated = summary.copy(
            healthEarned = e(PillarType.HEALTH),
            wealthEarned = e(PillarType.WEALTH),
            loveEarned = e(PillarType.LOVE),
            lifeEarned = e(PillarType.LIFE),
        )
        return updated.copy(balanced = PillarType.scoredPillars.all { Points.isFull(updated.earned(it), updated.target(it)) })
    }
}
