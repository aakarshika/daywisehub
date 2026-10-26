package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Pattern lines on the Metrics tab (spec 9.2): a few plain counts over stored weeks, ticks and badges. A
 * line shows only when the data holds it up, and nothing reads meaning into a title (spec 7.1): mission
 * and pillar titles sit on chips. Each kind of line keeps its own cap; there's none over all of them,
 * since the tab folds what comes after the first few.
 */
object Patterns {

    /** Balanced weeks in a row before they get a line. */
    const val BALANCED_RUN = 2
    /** A mission full this many counted weeks running, by its own 80% rule, is going well. */
    const val GOING_WELL_RUN = 3
    /** Full weeks in a row before a pillar gets a line… */
    const val FULL_RUN = 3
    /** …and at most this many pillars get one. */
    const val MAX_RUN_LINES = 2
    /** A pillar is strong when it was full in [STRONG_FULL] or more of its last [STRONG_WEEKS] counted weeks. */
    const val STRONG_WEEKS = 4
    const val STRONG_FULL = 3
    /** A pillar is picking up when it was full in each of its last [PICKING_UP_WEEKS] counted weeks… */
    const val PICKING_UP_WEEKS = 2
    /** …after at most [PICKING_UP_EARLIER_FULL] full weeks in the [PICKING_UP_EARLIER] counted weeks before them. */
    const val PICKING_UP_EARLIER = 4
    const val PICKING_UP_EARLIER_FULL = 1
    /** Weekends in a row before they get a line… */
    const val WEEKEND_RUN = 2
    /** …each with this many items done by hand across its Saturday and Sunday. */
    const val WEEKEND_DONE = 2
    /** Last week is the best in a while only when it beat this many counted weeks or more. */
    const val BEST_WEEK_MIN = 4
    /** "Best in N weeks" up to this many weeks, then "in N months" (weeks / 4). */
    const val BEST_WEEK_MAX_WEEKS = 8
    /** Days running, each with a badge besides "Recharging day", before they get a line. */
    const val BADGE_RUN = 5
    /** The weekday line looks at this many weeks, this one included… */
    const val WEEKDAY_WEEKS = 8
    /** …and needs this many ticks in the pillar, more than half of them on one weekday… */
    const val WEEKDAY_MIN_DONE = 8
    /** …and at most this many pillars get one. */
    const val MAX_WEEKDAY_LINES = 2
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

    /** Counted weeks a pillar needs for strong and picking up: the walk back stops once each has this many. */
    private val COUNTED_WEEKS = maxOf(STRONG_WEEKS, PICKING_UP_WEEKS + PICKING_UP_EARLIER)

    private val pluralDays = listOf("Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays")

    /**
     * A run of weeks back from last week. [capped] when it reached [MAX_RUN_WEEKS] without ending: it then
     * reads "for a year or more". The mission lines' "full weeks in a row" and "going well" use it too.
     */
    data class Run(val weeks: Int, val capped: Boolean)

    /**
     * As of [today], whatever week is on screen, in spec 9.2's order. [byWeek] holds the finished weeks by
     * their Monday, and [opened] is PaceOffers.openedWeeks of the tasks read. [badgeDay] is true for a day
     * with a badge besides "Recharging day" (DayBadgeBook.counts), and false past the badges' look-back and
     * before the first stored day. Ticks of archived missions count too: they happened.
     */
    fun lines(
        today: LocalDate,
        byWeek: Map<LocalDate, WeekSummary>,
        allMissions: List<Mission>,
        days: DayTasks,
        history: History,
        opened: Set<LocalDate>,
        badgeDay: (LocalDate) -> Boolean,
    ): List<List<LinePart>> {
        val thisWeek = WeekMath.weekStart(today)
        val lines = mutableListOf<List<LinePart>>()
        val balanced = runBack(thisWeek, byWeek, opened) { it.balanced }
        if (balanced.weeks >= BALANCED_RUN) {
            lines += words(if (balanced.capped) "Balanced weeks in a row for a year or more." else "${balanced.weeks} balanced weeks in a row.")
        }
        goingWell(thisWeek, allMissions, history, opened)?.let { lines += it }
        // A pillar with nothing due isn't a full week here. A balanced run already covers a shorter pillar run.
        val runs = PillarType.scoredPillars
            .map { pillar ->
                pillar to runBack(thisWeek, byWeek, opened) { it.target(pillar) > 0 && Points.isFull(it.earned(pillar), it.target(pillar)) }
            }
            .filter { (_, run) -> run.weeks >= FULL_RUN && run.weeks > balanced.weeks }
            .sortedByDescending { it.second.weeks }
            .take(MAX_RUN_LINES)
        runs.forEach { (pillar, run) ->
            lines += listOf(chip(pillar), LinePart(if (run.capped) " has been full for a year or more." else " has been full ${run.weeks} weeks running."))
        }
        // A pillar with a run line says it already, so strong and picking up leave it out.
        val withRun = runs.mapTo(mutableSetOf()) { it.first }
        val counted = countedWeeks(thisWeek, byWeek, opened)
        // With 3 or more balanced weeks running, every pillar would be strong.
        if (balanced.weeks < STRONG_FULL) {
            val strong = PillarType.scoredPillars.filter { pillar ->
                val weeks = counted.getValue(pillar)
                pillar !in withRun && weeks.size >= STRONG_WEEKS && weeks.take(STRONG_WEEKS).count { it } >= STRONG_FULL
            }
            if (strong.isNotEmpty()) lines += named(strong, " has been strong this past month.", " have been strong this past month.")
        }
        val pickingUp = PillarType.scoredPillars.filter { pillar ->
            val weeks = counted.getValue(pillar)
            pillar !in withRun && weeks.size >= PICKING_UP_WEEKS + PICKING_UP_EARLIER &&
                weeks.take(PICKING_UP_WEEKS).all { it } &&
                weeks.drop(PICKING_UP_WEEKS).take(PICKING_UP_EARLIER).count { it } <= PICKING_UP_EARLIER_FULL
        }
        if (pickingUp.isNotEmpty()) lines += named(pickingUp, " is picking up.", " are picking up.")
        val weekends = weekendRun(today, allMissions, days, opened)
        if (weekends >= WEEKEND_RUN) lines += words("$weekends weekends in a row with $WEEKEND_DONE or more things done.")
        bestWeek(thisWeek, byWeek, opened)?.let { lines += it }
        val badges = badgeRun(today, badgeDay)
        if (badges >= BADGE_RUN) lines += words("$badges days running with a badge besides ${Badge.RECHARGING_DAY.title}.")
        lines += weekdayLines(today, thisWeek, allMissions, days).take(MAX_WEEKDAY_LINES)
        return lines
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
     * "⟨Call mom⟩ and ⟨Gym⟩ are going well.": active missions full by their own 80% rule [GOING_WELL_RUN]
     * or more counted weeks running since their last pace change, the mission lines' own run
     * (MissionProgress.fullWeeks). Fixed blocks and Recharge are left out. Two are named, the longest runs
     * first and then in the mission lines' order, and the rest are counted.
     */
    private fun goingWell(thisWeek: LocalDate, allMissions: List<Mission>, history: History, opened: Set<LocalDate>): List<LinePart>? {
        val going = allMissions
            .filter { it.active && it.scored && it.missionKind != MissionKind.BLOCK }
            .sortedWith(compareBy({ it.pillarType.ordinal }, { it.missionId }))
            .map { it to MissionProgress.fullWeeks(it, thisWeek, history, opened).weeks }
            .filter { (_, weeks) -> weeks >= GOING_WELL_RUN }
            .sortedByDescending { (_, weeks) -> weeks }
            .map { (mission, _) -> LinePart(mission.title, mission.pillarType) }
        return when (going.size) {
            0 -> null
            1 -> listOf(going[0], LinePart(" is going well."))
            2 -> listOf(going[0], LinePart(" and "), going[1], LinePart(" are going well."))
            else -> listOf(going[0], LinePart(", "), going[1], LinePart(" and ${going.size - 2} more are going well."))
        }
    }

    /**
     * Each pillar's last counted weeks, newest first: true where it was full. A counted week is a finished
     * week with a stored row, not away, in which the pillar had something due; any other week is passed
     * over. It looks back [MAX_RUN_WEEKS] at most, and stops once every pillar has [COUNTED_WEEKS].
     */
    private fun countedWeeks(thisWeek: LocalDate, byWeek: Map<LocalDate, WeekSummary>, opened: Set<LocalDate>): Map<PillarType, List<Boolean>> {
        val counted = PillarType.scoredPillars.associateWith { mutableListOf<Boolean>() }
        var week = thisWeek.minus(7, DateTimeUnit.DAY)
        repeat(MAX_RUN_WEEKS) {
            val summary = byWeek[week]
            if (summary != null && !WeekScores.isAway(week, opened)) {
                counted.forEach { (pillar, full) ->
                    val target = summary.target(pillar)
                    if (target > 0 && full.size < COUNTED_WEEKS) full += Points.isFull(summary.earned(pillar), target)
                }
                if (counted.values.all { it.size >= COUNTED_WEEKS }) return counted
            }
            week = week.minus(7, DateTimeUnit.DAY)
        }
        return counted
    }

    /**
     * Weekends in a row with [WEEKEND_DONE] or more items done by hand across Saturday and Sunday, as the
     * badges count them: ticked, and not by the evening check. It counts back from the last finished
     * weekend, within [LOOK_BACK_DAYS], and this weekend counts too once it qualifies. A weekend in an away
     * week neither counts nor ends the run.
     */
    private fun weekendRun(today: LocalDate, allMissions: List<Mission>, days: DayTasks, opened: Set<LocalDate>): Int {
        val ids = allMissions.mapTo(mutableSetOf()) { it.missionId }
        fun byHand(day: LocalDate): Int =
            if (day > today) 0 else days.on(day).count { it.missionId in ids && it.taskState == TaskState.DONE && !it.autoTicked }
        fun qualifies(sunday: LocalDate): Boolean = byHand(sunday.minus(1, DateTimeUnit.DAY)) + byHand(sunday) >= WEEKEND_DONE
        val weekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
        var run = if (weekend && qualifies(WeekMath.weekEnd(today))) 1 else 0
        val floor = today.minus(LOOK_BACK_DAYS, DateTimeUnit.DAY)
        // The Sunday before today: last week's, on a Sunday too.
        var sunday = today.minus(today.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
        while (sunday.minus(1, DateTimeUnit.DAY) >= floor) {
            if (!WeekScores.isAway(WeekMath.weekStart(sunday), opened)) {
                if (!qualifies(sunday)) break
                run++
            }
            sunday = sunday.minus(7, DateTimeUnit.DAY)
        }
        return run
    }

    /**
     * "Last week was your best in 6 weeks.": last week's points, extra included, beat each of the
     * [BEST_WEEK_MIN] or more counted weeks before it. Counted weeks have a stored row and aren't away; the
     * others are passed over, neither beaten nor ending the walk. N is calendar weeks from the nearest
     * earlier week with as many points or more to last week, so the weeks passed over are in it. With none
     * by the first stored week or the run cap, it's the best yet. A week with no points is never a best.
     */
    private fun bestWeek(thisWeek: LocalDate, byWeek: Map<LocalDate, WeekSummary>, opened: Set<LocalDate>): List<LinePart>? {
        val lastWeek = thisWeek.minus(7, DateTimeUnit.DAY)
        val last = byWeek[lastWeek]?.takeIf { !WeekScores.isAway(lastWeek, opened) } ?: return null
        val points = points(last)
        if (points == 0) return null
        // Back to the first stored week, inside the runs' reach.
        val floor = maxOf(byWeek.keys.min(), thisWeek.minus(7 * MAX_RUN_WEEKS, DateTimeUnit.DAY))
        var week = lastWeek.minus(7, DateTimeUnit.DAY)
        var beaten = 0
        while (week >= floor) {
            val summary = byWeek[week]
            if (summary != null && !WeekScores.isAway(week, opened)) {
                if (points(summary) >= points) {
                    if (beaten < BEST_WEEK_MIN) return null
                    val weeks = week.daysUntil(lastWeek) / 7
                    return words(
                        if (weeks <= BEST_WEEK_MAX_WEEKS) "Last week was your best in $weeks weeks."
                        else "Last week was your best in ${weeks / 4} months."
                    )
                }
                beaten++
            }
            week = week.minus(7, DateTimeUnit.DAY)
        }
        return if (beaten >= BEST_WEEK_MIN) words("Last week was your best yet.") else null
    }

    /** A week's points over the scored pillars, extra included. */
    private fun points(summary: WeekSummary): Int = PillarType.scoredPillars.sumOf { summary.earned(it) }

    /**
     * Days running with a badge besides "Recharging day", back from today, or from yesterday while today
     * has none yet. [badgeDay] ends it at the badges' look-back and the first stored day.
     */
    private fun badgeRun(today: LocalDate, badgeDay: (LocalDate) -> Boolean): Int {
        val floor = today.minus(LOOK_BACK_DAYS, DateTimeUnit.DAY)
        var day = if (badgeDay(today)) today else today.minus(1, DateTimeUnit.DAY)
        var run = 0
        while (day >= floor && badgeDay(day)) {
            run++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return run
    }

    /**
     * "⟨Love⟩ mostly happens on Mondays": in the last [WEEKDAY_WEEKS] weeks the pillar has at least
     * [WEEKDAY_MIN_DONE] ticks, and more than half of them fall on one weekday, so "mostly" holds and no
     * other day ties it. Only missions free to land on any day count: fixed blocks, daily missions and
     * missions with preferred days are left out, since their days are set. The most lopsided come first.
     */
    private fun weekdayLines(today: LocalDate, thisWeek: LocalDate, allMissions: List<Mission>, days: DayTasks): List<List<LinePart>> {
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
            .map { (pillar, weekday, _) -> listOf(chip(pillar), LinePart(" mostly happens on ${pluralDays[weekday]}.")) }
    }

    /** Not a fixed block, not every day, and no preferred days: which weekday it lands on is up to you. */
    private fun freeDay(mission: Mission): Boolean =
        mission.missionKind != MissionKind.BLOCK && mission.periodUnit != Period.DAY && mission.daySet.isEmpty()

    private fun words(text: String): List<LinePart> = listOf(LinePart(text))

    private fun chip(pillar: PillarType) = LinePart(pillar.title, pillar)

    /** "⟨Life⟩[one]", "⟨Life⟩ and ⟨Health⟩[many]", "⟨Health⟩, ⟨Love⟩ and ⟨Life⟩[many]". */
    private fun named(pillars: List<PillarType>, one: String, many: String): List<LinePart> {
        val parts = mutableListOf<LinePart>()
        pillars.forEachIndexed { i, pillar ->
            if (i > 0) parts += LinePart(if (i == pillars.lastIndex) " and " else ", ")
            parts += chip(pillar)
        }
        parts += LinePart(if (pillars.size == 1) one else many)
        return parts
    }
}
