package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** One pillar's points against its target for one week (spec 5). */
data class PillarScore(
    val pillar: PillarType,
    val earned: Int,
    val target: Int,
    /**
     * What the close rule counts (spec 9.2): the pillar's stored rows that week that didn't move away, of
     * scored missions, or the occurrences due that week when more were due than there are rows.
     */
    val items: Int,
    /** The pillar's rows that week done or done in part. */
    val done: Int,
    /** The ring is close enough to be drawn closed (Points.closes). Never in an away week. */
    val closed: Boolean,
) {
    /** 80% of the target or more; nothing due counts as full. */
    val full: Boolean get() = Points.isFull(earned, target)
    /** Closed by the item count while under 80% of its points: the ring reads "nearly" instead of "light". */
    val nearly: Boolean get() = closed && !full
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
     * Either way, whether a ring closes comes from the week's stored rows, against what was due (items).
     * An away week never closes. [opened] is PaceOffers.openedWeeks of the same tasks.
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
        val weekTasks = days.between(weekStart, weekStart.plus(6, DateTimeUnit.DAY))
        val rows = closeRows(weekTasks, allMissions)
        val pillars = PillarType.scoredPillars.map { pillar ->
            val earned = summary?.earned(pillar) ?: Points.earnedInWeek(allMissions, weekTasks, pillar, weekStart)
            val target = summary?.target(pillar) ?: Points.weeklyTarget(missions, pillar, weekStart, history)
            val states = rows[pillar].orEmpty()
            // What was due counts too, so a ring doesn't close on the few rows of a week still under way, or of
            // one the diary was opened on only a couple of days. The rows cover archived missions.
            val items = maxOf(states.size, due(missions, pillar, weekStart, history))
            val done = states.count { it.countsAsDone }
            PillarScore(pillar, earned, target, items, done, closed = !away && Points.closes(earned, target, items, done))
        }
        return WeekScore(pillars, summary?.balanced ?: balanced(pillars), finished, away)
    }

    /**
     * The states of [weekTasks] the close rule counts, by pillar: rows of scored missions that didn't move
     * away. [allMissions] include archived ones, so every row keeps its pillar.
     */
    private fun closeRows(weekTasks: List<TodayTask>, allMissions: List<Mission>): Map<PillarType, List<TaskState>> {
        val byId = allMissions.associateBy { it.missionId }
        return weekTasks
            .filter { it.taskState != TaskState.MOVED }
            .mapNotNull { task -> byId[task.missionId]?.takeIf { it.scored }?.let { it.pillarType to task.taskState } }
            .groupBy({ it.first }, { it.second })
    }

    /** How many times [pillar]'s active scored [missions] are due in the week, by the live target's count. */
    private fun due(missions: List<Mission>, pillar: PillarType, weekStart: LocalDate, history: History): Int =
        missions.filter { it.pillarType == pillar && it.scored }.sumOf { Points.occurrencesInWeek(it, weekStart, history) }

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
