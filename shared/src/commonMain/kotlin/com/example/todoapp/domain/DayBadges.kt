package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** How rare a badge is. The sticker's fill follows it. */
enum class BadgeTier { EVERYDAY, GOOD_DAY, RARE, RAREST }

/**
 * Spec 9.3: what a day earned, commonest first. The order is the rank: a later badge is rarer, the
 * calendar shows a day's rarest, and lists show them rarest first. Badges are worked out from stored
 * data every time and never stored, so entries can be added or moved freely.
 */
enum class Badge(val title: String, val tier: BadgeTier) {
    RECHARGING_DAY("Recharging day", BadgeTier.EVERYDAY),
    DID_SOME("Did some", BadgeTier.EVERYDAY),
    FIRST_ONE("First one", BadgeTier.EVERYDAY),
    STRONG_START("Strong start", BadgeTier.EVERYDAY),
    RESTED("Rested on purpose", BadgeTier.EVERYDAY),
    HALFWAY("Halfway", BadgeTier.EVERYDAY),
    TWO_IN_A_ROW("Two in a row", BadgeTier.EVERYDAY),
    SHOWED_UP("Showed up", BadgeTier.EVERYDAY),
    BONUS_ROUND("Bonus round", BadgeTier.EVERYDAY),
    SECOND_CHANCE("Second chance", BadgeTier.EVERYDAY),
    HAT_TRICK("Hat trick", BadgeTier.GOOD_DAY),
    CROWNED("Crowned", BadgeTier.GOOD_DAY),
    HEAVY_LIFTING("Heavy lifting", BadgeTier.GOOD_DAY),
    FULL_ENOUGH("Full enough", BadgeTier.GOOD_DAY),
    COUNT_IT("We'll count it", BadgeTier.GOOD_DAY),
    WEEKEND_WARRIOR("Weekend warrior", BadgeTier.GOOD_DAY),
    BACK_AT_IT("Back at it", BadgeTier.GOOD_DAY),
    OVERDUE_NO_MORE("Overdue no more", BadgeTier.GOOD_DAY),
    GENTLE_WIN("Gentle win", BadgeTier.GOOD_DAY),
    ALL_FOUR("All four", BadgeTier.GOOD_DAY),
    ALL_THREE_CROWNS("All three crowns", BadgeTier.RARE),
    CLEAN_SWEEP("Clean sweep", BadgeTier.RARE),
    MILESTONE("Milestone", BadgeTier.RARE),
    PILLAR_FILLED("Pillar filled", BadgeTier.RARE),
    THE_RETURN("The return", BadgeTier.RARE),
    SEVEN_DAY_RUN("7-day run", BadgeTier.RARE),
    FINISH_LINE("Finish line", BadgeTier.RAREST),
    RE_RETURN("The re-return", BadgeTier.RAREST),
    WEEK_MAKER("Week maker", BadgeTier.RAREST),
    THIRTY_DAY_RUN("30-day run", BadgeTier.RAREST),
    PERSONAL_BEST("Personal best", BadgeTier.RAREST),
}

/** A badge with the line under it, built from stored facts only. A mission title sits on a chip (spec 7.1). */
data class EarnedBadge(val badge: Badge, val reason: List<LinePart>)

/** What a day's badges read. [DayBadges.book] reads it once, then answers any day from [from] to [to]. */
data class BadgeInput(
    val today: LocalDate,
    /** The first and last day that will be asked about. */
    val from: LocalDate,
    val to: LocalDate,
    /** Active and archived missions: a done item keeps its mission's pillar and kind after archiving. */
    val allMissions: List<Mission>,
    /**
     * Tasks from [lookBackDays] before [from] to the end of [to]'s week, or more. Live weekly targets read
     * it as the diary's bars do.
     */
    val history: History,
    /** Day plans covering [from] to [to], for each day's energy. */
    val plans: List<DayPlan>,
    /** Every milestone, archived missions' too. */
    val milestones: List<Milestone>,
    /** Finished weeks by their Monday. A week without one is worked out live. */
    val summaries: Map<LocalDate, WeekSummary>,
    /** The first stored item's day; null on an empty install. No day before it gets a badge. */
    val firstDay: LocalDate?,
    /**
     * How far back runs, personal bests, returns and quiet days look: PlanRepository.HISTORY_DAYS, which
     * is what the diary reads, so the diary and the Metrics tab agree about every day.
     */
    val lookBackDays: Int,
    /** PlanRepository.MAX_CROWNS. */
    val maxCrowns: Int,
)

/** Spec 9.3: badges for a day, worked out from what the app already stores. */
object DayBadges {

    /** The shortest cycle, in days, that can earn "The return" and "The re-return". */
    const val RETURN_MIN_CYCLE = 28

    fun book(input: BadgeInput): DayBadgeBook = DayBadgeBook(input)
}

/** Daily habits, the way the planner's streak line counts them. */
private val Mission.daily: Boolean
    get() = (missionKind == MissionKind.HABIT || missionKind == MissionKind.FINISH_LINE) && periodUnit == Period.DAY

/**
 * Once a month or slower, by the planner's own interval: a rhythm of 28 days or more, or a habit once a
 * month. Recharge never counts as missed, so it has no return.
 */
private val Mission.monthlyish: Boolean
    get() = repeatsByInterval && missionKind != MissionKind.RECHARGE && intervalDays() >= DayBadges.RETURN_MIN_CYCLE

/**
 * Every task, plan and milestone a day's badges need, indexed in one pass, so a month of days never
 * reads every task again. Each task's date is read once. Make one per screen state.
 */
class DayBadgeBook internal constructor(private val input: BadgeInput) {

    private val today = input.today
    private val thisWeek = WeekMath.weekStart(today)
    private val lookBack = input.lookBackDays
    private val byId = input.allMissions.associateBy { it.missionId }
    /** Live weekly targets count active missions only, as the diary's bars do. */
    private val active = input.allMissions.filter { it.active }

    private val rows = mutableMapOf<LocalDate, MutableList<TodayTask>>()
    /** The days each mission was done or done in part: the planner's streak and due-date rule. */
    private val doneDays = mutableMapOf<Long, MutableSet<LocalDate>>()
    /** Days with something done or done in part by hand, or a milestone reached. Every other day is quiet. */
    private val handDays = mutableSetOf<LocalDate>()
    /** Days with something done by hand: ticked, not done in part. */
    private val tickDays = mutableSetOf<LocalDate>()
    private val lowEnergy: Set<LocalDate>
    private val completedOn: Map<LocalDate, List<Milestone>>
    private val milestonesOf: Map<Long, List<Milestone>>
    /** Each week's pillars, worked out the first time one of its days is asked about. */
    private val weeks = mutableMapOf<LocalDate, WeekFill>()

    init {
        // yyyy-MM-dd: text order is date order, so tasks outside what any day can look at are skipped unread.
        val fromKey = MyDate.fromLocalDate(input.from.minus(lookBack, DateTimeUnit.DAY)).dateString
        val toKey = MyDate.fromLocalDate(WeekMath.weekEnd(input.to)).dateString
        for (task in input.history.all) {
            val key = task.taskDate.dateString
            if (key < fromKey || key > toKey) continue
            val date = task.date
            rows.getOrPut(date) { mutableListOf() } += task
            val state = task.taskState
            if (!state.countsAsDone) continue
            doneDays.getOrPut(task.missionId) { mutableSetOf() } += date
            // The evening check's ticks ("Ticked for you") aren't done by hand, or a work block would fill every weekday.
            if (!task.autoTicked) {
                handDays += date
                if (state == TaskState.DONE) tickDays += date
            }
        }
        val fromDay = MyDate.fromLocalDate(input.from).dateString
        val toDay = MyDate.fromLocalDate(input.to).dateString
        lowEnergy = input.plans
            .filter { it.energy == Energy.LOW.name && it.date >= fromDay && it.date <= toDay }
            .mapTo(mutableSetOf()) { MyDate(it.date).toLocalDate() }
        val completed = input.milestones.filter {
            it.status == TaskStatus.COMPLETED.value && it.actualCompletionDate != null && it.missionId in byId
        }
        completedOn = completed.groupBy { it.actualCompletionDate!!.toLocalDate() }
        // A milestone reached counts as doing something, though it isn't an item: the day keeps Milestone (and
        // Finish line) instead of the fallback, and a quiet stretch ends there.
        handDays += completedOn.keys
        milestonesOf = input.milestones.groupBy { it.missionId }
    }

    /**
     * [date]'s badges, rarest first. A finished day with nothing done by hand and no milestone reached gets
     * only "Recharging day",
     * and so does an away week's day: its quiet shows without a word about missing anything. Today has
     * the ones earned so far and never "Recharging day". Days after today, days before the first stored
     * item and days outside the input's range get none.
     */
    fun on(date: LocalDate): List<EarnedBadge> {
        val first = input.firstDay ?: return emptyList()
        if (date < first || date > today || date < input.from || date > input.to) return emptyList()
        if (date !in handDays) {
            return if (date < today) listOf(EarnedBadge(Badge.RECHARGING_DAY, words("A quiet day"))) else emptyList()
        }
        val day = rows[date].orEmpty().filter { it.missionId in byId }
        val earned = mutableListOf<EarnedBadge>()
        fun add(badge: Badge, reason: List<LinePart>) {
            earned += EarnedBadge(badge, reason)
        }
        val ticked = day.filter { it.taskState == TaskState.DONE }
        // Done by hand: ticked, but not by the evening check.
        val byHand = ticked.filter { !it.autoTicked }
        // Picks: what the day holds besides its fixed blocks, minus what moved away.
        val picks = day.filter { !it.isFixedBlock && it.taskState != TaskState.MOVED }
        val blocks = day.filter { it.isFixedBlock }
        // Open crowns only, as the diary counts them: a skipped item can keep its crown.
        val crowned = day.filter { it.crowned && it.taskState.isOpen }
        val weekday = date.dayOfWeek
        // Points over the scored picks: what they earned against everything they could earn.
        val scoredPicks = picks.filter { mission(it).scored }
        val points = scoredPicks.sumOf { it.pointsEarned }
        val planned = scoredPicks.sumOf { Points.forTask(mission(it), TaskState.DONE) }
        val fullEnough = planned > 0 && Points.isFull(points, planned)
        val floor = date.minus(lookBack, DateTimeUnit.DAY)

        picks.filter { it.taskState == TaskState.PARTIAL }.missions().takeIf { it.isNotEmpty() }?.let {
            add(Badge.DID_SOME, about("Did some of ", it, "", many = { n -> "Did some of $n things" }))
        }
        if (byHand.isNotEmpty()) add(Badge.FIRST_ONE, words("${byHand.size} done"))
        if (weekday == DayOfWeek.MONDAY && byHand.isNotEmpty()) add(Badge.STRONG_START, words("${byHand.size} done to start the week"))
        byHand.filter { mission(it).pillarType == PillarType.RECHARGE }.missions().takeIf { it.isNotEmpty() }?.let {
            add(Badge.RESTED, about("Made time for ", it, "", many = { n -> "Made time for $n restful things" }))
        }
        if (planned > 0 && points * 2 >= planned) add(Badge.HALFWAY, words("$points of ${MissionWords.pointsPhrase(planned)}"))
        if (date in tickDays && date.minus(1, DateTimeUnit.DAY) in tickDays) {
            add(Badge.TWO_IN_A_ROW, words("${runOf(tickDays, date, floor)} days in a row with something done"))
        }
        val others = ticked.count { !it.isFixedBlock }
        if (blocks.isNotEmpty() && blocks.all { it.taskState == TaskState.DONE } && others > 0) {
            val all = when (blocks.size) {
                1 -> "The fixed block"
                2 -> "Both fixed blocks"
                else -> "All ${blocks.size} fixed blocks"
            }
            add(Badge.SHOWED_UP, words("$all and $others more done"))
        }
        ticked.filter { it.reasonCode == ReasonCode.ADDED.name }.missions().takeIf { it.isNotEmpty() }?.let {
            add(Badge.BONUS_ROUND, about("Added and did ", it, "", many = { n -> "Added and did $n things" }))
        }
        ticked.filter { it.movedFromDate != null }.missions().takeIf { it.isNotEmpty() }?.let {
            add(Badge.SECOND_CHANCE, about("Did ", it, " after a move", many = { n -> "Did $n things after a move" }))
        }
        if (byHand.size >= 3) add(Badge.HAT_TRICK, words("${byHand.size} done in one day"))
        ticked.filter { it.crowned }.missions().takeIf { it.isNotEmpty() }?.let {
            val verb = if (it.size == 1) " was crowned and done" else " were crowned and done"
            add(Badge.CROWNED, about("", it, verb, many = { n -> "$n crowned and done" }))
        }
        byHand.filter { mission(it).effortLevel == Effort.DRAINING }.missions().takeIf { it.isNotEmpty() }?.let {
            val feel = if (it.size == 1) ", which feels draining" else ", which feel draining"
            add(Badge.HEAVY_LIFTING, about("Did ", it, feel, many = { n -> "Did $n draining things" }))
        }
        if (fullEnough) add(Badge.FULL_ENOUGH, words("$points of ${MissionWords.pointsPhrase(planned)} · full at ${(Points.FULL_SHARE * 100).toInt()}%"))
        val donePicks = picks.count { it.taskState == TaskState.DONE }
        if (picks.size >= 3 && donePicks == picks.size - 1) add(Badge.COUNT_IT, words("$donePicks of ${picks.size} done"))
        if ((weekday == DayOfWeek.SATURDAY || weekday == DayOfWeek.SUNDAY) && byHand.size >= 2) {
            add(Badge.WEEKEND_WARRIOR, words("${byHand.size} done on a ${date.weekdayName()}"))
        }
        if (date in tickDays) backAfter(date, first, floor)?.let { add(Badge.BACK_AT_IT, words(it)) }
        ticked.filter { it.reasonCode == ReasonCode.RHYTHM_OVERDUE.name }.missions().takeIf { it.isNotEmpty() }?.let {
            add(Badge.OVERDUE_NO_MORE, about("Caught up on ", it, "", many = { n -> "Caught up on $n things" }))
        }
        if (fullEnough && date in lowEnergy) add(Badge.GENTLE_WIN, words("A low-energy day, $points of ${MissionWords.pointsPhrase(planned)}"))
        val touched = byHand.mapTo(mutableSetOf()) { mission(it).pillarType }
        if (PillarType.scoredPillars.all { it in touched }) {
            add(Badge.ALL_FOUR, words("Something in ${names(PillarType.scoredPillars.map { it.title })}"))
        }
        if (crowned.size >= input.maxCrowns && crowned.all { it.taskState == TaskState.DONE }) {
            add(Badge.ALL_THREE_CROWNS, words("All ${crowned.size} crowned done"))
        }
        if (picks.size >= 2 && donePicks == picks.size) {
            add(Badge.CLEAN_SWEEP, words(if (picks.size == 2) "Both picks done" else "All ${picks.size} picks done"))
        }
        completedOn[date]?.let { add(Badge.MILESTONE, milestoneLine(it)) }
        val fill = week(WeekMath.weekStart(date))
        val dayIndex = WeekMath.weekStart(date).daysUntil(date)
        fill.filled[dayIndex].takeIf { it.isNotEmpty() }?.let { pillars ->
            add(Badge.PILLAR_FILLED, words("${names(pillars.map { it.title })} full for the week"))
        }
        val returns = ticked.missions().filter { it.monthlyish }.mapNotNull { gap(it, date, floor) }
        returns.filter { it.cyclesMissed == 1 }.maxByOrNull { it.days }?.let { add(Badge.THE_RETURN, it.line(lookBack)) }
        // Daily runs count did-some days too, as the streak on the reason line does.
        val runs = day.filter { it.taskState.countsAsDone }.missions().filter { it.daily }
            .associateWith { runOf(doneDays[it.missionId].orEmpty(), date, floor) }
        runs.filterValues { it == 7 }.keys.toList().takeIf { it.isNotEmpty() }?.let {
            add(Badge.SEVEN_DAY_RUN, about("", it, " 7 days running", many = { n -> "$n habits 7 days running" }))
        }
        milestonesDoneOn(date)?.let { (mission, count) ->
            val done = if (count == 1) "its milestone is done" else MissionWords.allMilestonesDone(count)
            add(Badge.FINISH_LINE, listOf(chip(mission), LinePart(" · $done")))
        }
        returns.filter { it.cyclesMissed >= 2 }.maxByOrNull { it.days }?.let { add(Badge.RE_RETURN, it.line(lookBack)) }
        if (fill.maker == dayIndex) {
            val all = "All ${PillarType.scoredPillars.size} pillars full"
            add(Badge.WEEK_MAKER, words(if (fill.finished) "$all, a balanced week" else "$all so far"))
        }
        runs.filterValues { it == 30 }.keys.toList().takeIf { it.isNotEmpty() }?.let {
            add(Badge.THIRTY_DAY_RUN, about("", it, " 30 days running", many = { n -> "$n habits 30 days running" }))
        }
        bestRun(runs, date, floor)?.let { (mission, run, best) ->
            add(Badge.PERSONAL_BEST, listOf(chip(mission), LinePart(" $run days running, past its best of $best")))
        }
        // Every finished day keeps at least one badge, even one whose only act earns nothing on its own.
        if (earned.isEmpty() && date < today) return listOf(EarnedBadge(Badge.RECHARGING_DAY, words("A quiet day")))
        return earned.sortedByDescending { it.badge.ordinal }
    }

    /**
     * True when [date] is inside the input's range and has something done or done in part by hand, or a
     * milestone reached: the fallback's own test in [on]. No badges are worked out for it, so a run of days
     * stays cheap. A day whose only act earns nothing on its own (a fixed block done in part) passes it,
     * though [on] still ends with the fallback.
     */
    fun counts(date: LocalDate): Boolean = date >= input.from && date <= input.to && date in handDays

    private fun mission(task: TodayTask): Mission = byId.getValue(task.missionId)

    /** Each mission once, in list order. */
    private fun List<TodayTask>.missions(): List<Mission> = distinctBy { it.missionId }.map { mission(it) }

    /** Days in a row in [days] up to [date], looking back no further than [floor]. */
    private fun runOf(days: Set<LocalDate>, date: LocalDate, floor: LocalDate): Int {
        var day = date
        var run = 0
        while (day >= floor && day in days) {
            run++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return run
    }

    /**
     * "Back at it": its reason when the 3 or more days before [date] were quiet and something was done by
     * hand before them. A quiet stretch that runs past the look-back, with items stored before it, is a long
     * break. Null otherwise, so the first day ever done isn't a comeback.
     */
    private fun backAfter(date: LocalDate, first: LocalDate, floor: LocalDate): String? {
        val from = maxOf(first, floor)
        var day = date.minus(1, DateTimeUnit.DAY)
        var quiet = 0
        while (day >= from && day !in handDays) {
            quiet++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        if (quiet < 3) return null
        if (day >= from) return "Back after $quiet quiet days"
        return if (first < from) "Back after a long break" else null
    }

    /**
     * A daily habit whose run passes, on [date], the longest earlier run in the look-back, when that was 3
     * days or more. It fires once: the run is then one day longer than the best before it.
     */
    private fun bestRun(runs: Map<Mission, Int>, date: LocalDate, floor: LocalDate): Triple<Mission, Int, Int>? =
        runs.mapNotNull { (mission, run) ->
            // An earlier run ends at least a day before this one starts.
            val best = longestRun(doneDays[mission.missionId].orEmpty(), floor, date.minus(run + 1, DateTimeUnit.DAY))
            Triple(mission, run, best).takeIf { best >= 3 && run == best + 1 }
        }.maxByOrNull { it.second }

    /** The longest run of days in [days] from [floor] to [until]. */
    private fun longestRun(days: Set<LocalDate>, floor: LocalDate, until: LocalDate): Int {
        var best = 0
        var run = 0
        var day = floor
        while (day <= until) {
            run = if (day in days) run + 1 else 0
            best = maxOf(best, run)
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return best
    }

    /** How long a once-a-month-or-slower mission waited before [date]'s tick, in its own cycles. */
    private class Gap(val mission: Mission, val days: Int, val cyclesMissed: Int, val since: Since) {
        enum class Since { LAST_DONE, START, BEYOND_LOOK_BACK }

        fun line(lookBack: Int): List<LinePart> = listOf(
            chip(mission),
            LinePart(
                when (since) {
                    Since.LAST_DONE -> " done for the first time in $days days"
                    Since.START -> " done for the first time, $days days after it started"
                    Since.BEYOND_LOOK_BACK -> " done for the first time in over $lookBack days"
                }
            ),
        )
    }

    /**
     * Cycles missed = gap / cycle - 1. The gap runs from the last time it was done or done in part, as its
     * due date does, or from its start. When neither is in the look-back, the gap is longer than the
     * look-back: that's still a re-return when even the shortest such gap misses 2 cycles, and can't be
     * told otherwise.
     */
    private fun gap(mission: Mission, date: LocalDate, floor: LocalDate): Gap? {
        val last = doneDays[mission.missionId].orEmpty().filter { it < date && it >= floor }.maxOrNull()
        val start = mission.startDate.toLocalDate()
        val (days, since) = when {
            last != null -> last.daysUntil(date) to Gap.Since.LAST_DONE
            start >= floor -> start.daysUntil(date) to Gap.Since.START
            else -> lookBack + 1 to Gap.Since.BEYOND_LOOK_BACK
        }
        val missed = days / mission.intervalDays() - 1
        // Past the look-back only the shortest gap is known: enough to tell a re-return, not a return.
        val told = since != Gap.Since.BEYOND_LOOK_BACK || missed >= 2
        return Gap(mission, days, missed, since).takeIf { told && missed >= 1 }
    }

    /** "Milestone 2 of 3 for ⟨Dance class⟩", "2 milestones for ⟨Dance class⟩". Milestone texts aren't shown. */
    private fun milestoneLine(done: List<Milestone>): List<LinePart> {
        val missions = done.distinctBy { it.missionId }.mapNotNull { byId[it.missionId] }
        if (done.size == 1 && missions.size == 1) {
            val mine = milestonesOf[done[0].missionId].orEmpty().sortedBy { it.milestoneOrder }
            val place = mine.indexOfFirst { it.milestoneId == done[0].milestoneId } + 1
            return listOf(LinePart("Milestone $place of ${mine.size} for "), chip(missions[0]))
        }
        return about("${done.size} milestones for ", missions, "", many = { "${done.size} milestones" })
    }

    /** A finish line whose last open milestone was done on [date], with how many it has. */
    private fun milestonesDoneOn(date: LocalDate): Pair<Mission, Int>? =
        completedOn[date].orEmpty()
            .mapNotNull { byId[it.missionId] }
            .distinctBy { it.missionId }
            .filter { it.missionKind == MissionKind.FINISH_LINE }
            .firstNotNullOfOrNull { mission ->
                val mine = milestonesOf[mission.missionId].orEmpty()
                val dates = mine.map { if (it.status == TaskStatus.COMPLETED.value) it.actualCompletionDate?.toLocalDate() else null }
                if (dates.any { it == null } || dates.filterNotNull().max() != date) null else mission to mine.size
            }

    /** A week's pillars day by day. */
    private class WeekFill(
        /** By day of the week (0 is Monday): the pillars whose points first reached full that day. */
        val filled: List<List<PillarType>>,
        /** The day by which every pillar was full, when the week is balanced. */
        val maker: Int?,
        /** The week is over, so its balance is final. */
        val finished: Boolean,
    )

    /**
     * Targets come from the stored summary of a finished week, else live, as the diary's bars and the
     * Metrics rings work them out. Points add up the week's own tasks day by day, archived missions' too.
     */
    private fun week(monday: LocalDate): WeekFill = weeks.getOrPut(monday) {
        val pillars = PillarType.scoredPillars
        val stored = input.summaries[monday]?.takeIf { monday < thisWeek }
        val target = pillars.map { stored?.target(it) ?: Points.weeklyTarget(active, it, monday, input.history) }
        val running = IntArray(pillars.size)
        val byDay = (0..6).map { offset ->
            for (task in rows[monday.plus(offset, DateTimeUnit.DAY)].orEmpty()) {
                val pillar = byId[task.missionId]?.takeIf { it.scored }?.pillarType ?: continue
                running[pillars.indexOf(pillar)] += task.pointsEarned
            }
            running.copyOf()
        }
        fun full(day: Int, p: Int) = day >= 0 && Points.isFull(byDay[day][p], target[p])
        val filled = (0..6).map { day ->
            pillars.indices.filter { p -> target[p] > 0 && full(day, p) && !full(day - 1, p) }.map { pillars[it] }
        }
        // The crown's own rule: stored for a finished week, live (so far) for this one.
        val balanced = stored?.balanced ?: WeekSummaries.balanced({ byDay[6][pillars.indexOf(it)] }, { target[pillars.indexOf(it)] })
        val maker = if (balanced) (0..6).firstOrNull { day -> pillars.indices.all { full(day, it) } } else null
        WeekFill(filled, maker, finished = monday < thisWeek)
    }

    private companion object {
        fun words(text: String): List<LinePart> = listOf(LinePart(text))

        fun chip(mission: Mission) = LinePart(mission.title, mission.pillarType)

        /** "Health", "Health and Love", "Health, Wealth, Love and Life". */
        fun names(names: List<String>): String =
            if (names.size <= 1) names.joinToString() else names.dropLast(1).joinToString(", ") + " and " + names.last()

        /**
         * [prefix], one or two missions on chips, then [suffix]: "Did ⟨Run⟩ and ⟨Swim⟩ after a move". Three
         * or more are counted instead ([many]), so the line stays short.
         */
        fun about(prefix: String, missions: List<Mission>, suffix: String, many: (Int) -> String): List<LinePart> {
            val chips = when (missions.size) {
                1 -> listOf(chip(missions[0]))
                2 -> listOf(chip(missions[0]), LinePart(" and "), chip(missions[1]))
                else -> return words(many(missions.size))
            }
            return (listOf(LinePart(prefix)) + chips + LinePart(suffix)).filter { it.text.isNotEmpty() }
        }
    }
}
