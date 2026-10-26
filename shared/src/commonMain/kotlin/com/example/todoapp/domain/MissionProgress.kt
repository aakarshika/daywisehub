package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt

/** One mission's line on the Metrics tab (spec 9). */
data class MissionLine(
    val missionId: Long,
    val title: String,
    val pillar: PillarType,
    val finishLine: Boolean,
    /** "26 of 30 days this month · best streak 14", "Every 12 days on average · you aim for every 2 weeks" */
    val detail: String,
)

/**
 * One progress line per active mission, as of today, from stored data only (spec 7.1): kind, pace,
 * done days, milestones. Archived missions are left out; their points still count in the weeks they
 * were earned in.
 */
object MissionProgress {

    /**
     * [opened] is PaceOffers.openedWeeks of the tasks read. Only done days in the last
     * Patterns.LOOK_BACK_DAYS count, so browsing an older month (which reads further back) changes nothing.
     */
    fun lines(
        today: LocalDate,
        missions: List<Mission>,
        history: History,
        milestones: List<Milestone>,
        opened: Set<LocalDate>,
    ): List<MissionLine> {
        val thisWeek = WeekMath.weekStart(today)
        val since = today.minus(Patterns.LOOK_BACK_DAYS, DateTimeUnit.DAY)
        val milestonesById = milestones.groupBy { it.missionId }
        return missions
            .sortedWith(compareBy({ it.pillarType.ordinal }, { it.missionId }))
            .map { mission ->
                // The mission's done days, each date read once.
                val done = history.tasksFor(mission.missionId)
                    .filter { it.taskState.countsAsDone }
                    .map { it.date }
                    .filter { it >= since }
                    .distinct()
                    .sorted()
                MissionLine(
                    missionId = mission.missionId,
                    title = mission.title,
                    pillar = mission.pillarType,
                    finishLine = mission.missionKind == MissionKind.FINISH_LINE,
                    detail = detail(mission, done, today, thisWeek, history, milestonesById[mission.missionId].orEmpty(), opened),
                )
            }
    }

    private fun detail(
        mission: Mission,
        done: List<LocalDate>,
        today: LocalDate,
        thisWeek: LocalDate,
        history: History,
        milestones: List<Milestone>,
        opened: Set<LocalDate>,
    ): String {
        notLive(mission, today)?.let { return it }
        val weekEnd = thisWeek.plus(6, DateTimeUnit.DAY)
        // "This week" counts keep later days of the week; lines as of today leave out anything after it.
        val untilToday = done.takeWhile { it <= today }
        return when {
            mission.missionKind == MissionKind.BLOCK -> {
                val due = Points.occurrencesInWeek(mission, thisWeek, history)
                if (due == 0) "Nothing due this week" else "${count(done, thisWeek, weekEnd)} of $due this week"
            }
            mission.missionKind == MissionKind.RECHARGE ->
                "${count(done, thisWeek, weekEnd)} this week · about ${MissionWords.times(mission.count)} a week"
            mission.missionKind == MissionKind.RHYTHM -> rhythm(mission, untilToday, today)
            mission.missionKind == MissionKind.FINISH_LINE && milestones.isNotEmpty() -> milestone(milestones)
            mission.periodUnit == Period.DAY -> daily(mission, untilToday, today)
            mission.periodUnit == Period.MONTH ->
                "${count(done, LocalDate(today.year, today.month, 1), today)} of ${mission.count} this month"
            else -> weekly(mission, done, thisWeek, history, opened)
        }
    }

    /** Paused, not started yet, or past its end date. */
    private fun notLive(mission: Mission, today: LocalDate): String? {
        mission.pausedUntil?.toLocalDate()?.let { if (it > today) return "Paused until ${it.shortLabel()}" }
        val start = mission.startDate.toLocalDate()
        if (start > today) return "Starts ${start.shortLabel()}"
        mission.endDate?.toLocalDate()?.let { if (it < today) return "Ended ${it.shortLabel()}" }
        return null
    }

    /**
     * "26 of 30 days this month · best streak 14". Today counts once it's done, so a morning isn't a day
     * missed. [done] stops at today.
     */
    private fun daily(mission: Mission, done: List<LocalDate>, today: LocalDate): String {
        val from = maxOf(LocalDate(today.year, today.month, 1), mission.startDate.toLocalDate())
        val through = if (done.lastOrNull() == today) today else today.minus(1, DateTimeUnit.DAY)
        val dayCount = if (through < from) 0 else from.daysUntil(through) + 1
        val best = bestStreak(done)
        val streak = if (best >= 2) " · best streak $best" else ""
        if (dayCount == 0) return "Every day$streak"
        return "${count(done, from, through)} of $dayCount ${if (dayCount == 1) "day" else "days"} this month$streak"
    }

    /** "Every 12 days on average · you aim for every 2 weeks". [done] stops at today. */
    private fun rhythm(mission: Mission, done: List<LocalDate>, today: LocalDate): String {
        val aim = "you aim for every ${mission.intervalLabel()}"
        return when (done.size) {
            0 -> "Not done yet · $aim"
            1 -> {
                val ago = done.last().daysUntil(today)
                val last = when (ago) {
                    0 -> "Last done today"
                    1 -> "Last done yesterday"
                    else -> "Last done $ago days ago"
                }
                "$last · $aim"
            }
            else -> {
                val average = (done.first().daysUntil(done.last()).toFloat() / (done.size - 1)).roundToInt()
                "Every ${if (average == 1) "day" else "$average days"} on average · $aim"
            }
        }
    }

    /** "Milestone 2 of 3 · due Dec 20", as the MILESTONE reason line puts it. Milestone texts aren't shown. */
    private fun milestone(milestones: List<Milestone>): String {
        val mine = milestones.sortedBy { it.milestoneOrder }
        val next = mine.indexOfFirst { it.status != TaskStatus.COMPLETED.value }
        if (next < 0) return MissionWords.allMilestonesDone(mine.size)
        return "Milestone ${next + 1} of ${mine.size} · due ${mine[next].expectedCompletionDate.toLocalDate().shortLabel()}"
    }

    /** "2 of 3 this week", then the gentle pace or a run of full weeks. */
    private fun weekly(mission: Mission, done: List<LocalDate>, thisWeek: LocalDate, history: History, opened: Set<LocalDate>): String {
        val target = Points.occurrencesInWeek(mission, thisWeek, history)
        if (target == 0) return "Nothing due this week"
        val base = "${count(done, thisWeek, thisWeek.plus(6, DateTimeUnit.DAY))} of $target this week"
        val goal = mission.goalCount
        if (goal != null && goal > mission.count) {
            return "$base · ${if (mission.count == 1) "once a week" else "${mission.count}× a week"} for now"
        }
        val run = fullWeeks(mission, thisWeek, history, opened)
        return when {
            run.weeks < 2 -> base
            run.capped -> "$base · full every week for a year or more"
            else -> "$base · ${run.weeks} full weeks in a row"
        }
    }

    /**
     * Finished weeks in a row, back from last week, that were full the way pace offers judge it
     * (PaceOffers.full): 80% of the mission's own points, a did-some earning half. A week it wasn't due
     * ends the run, and so does its last pace change (Mission.paceSince): only weeks starting on or after
     * it count. An away week (no items of its own) neither counts nor ends it.
     */
    private fun fullWeeks(mission: Mission, thisWeek: LocalDate, history: History, opened: Set<LocalDate>): Patterns.Run {
        val since = mission.paceSince?.let { MyDate(it).toLocalDate() }
        var week = thisWeek.minus(7, DateTimeUnit.DAY)
        var run = 0
        repeat(Patterns.MAX_RUN_WEEKS) {
            if (since != null && week < since) return Patterns.Run(run, capped = false)
            if (!WeekScores.isAway(week, opened)) {
                if (!PaceOffers.full(mission, week, history)) return Patterns.Run(run, capped = false)
                run++
            }
            week = week.minus(7, DateTimeUnit.DAY)
        }
        return Patterns.Run(run, capped = true)
    }

    /** Longest run of consecutive done days in [done], which is sorted and has no repeats. */
    private fun bestStreak(done: List<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in done) {
            run = if (previous != null && previous.plus(1, DateTimeUnit.DAY) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    private fun count(done: List<LocalDate>, from: LocalDate, to: LocalDate): Int = done.count { it >= from && it <= to }
}
