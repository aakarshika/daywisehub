package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.TaskStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class PlanInput(
    val date: LocalDate,
    val missions: List<Mission>,
    /** Archived missions too, so points they earned this week still count toward a pillar's gap. */
    val earnedMissions: List<Mission> = missions,
    val milestones: List<Milestone>,
    /** Tasks from about 120 days back through a week ahead, including today's. */
    val history: History,
    val todayTasks: List<TodayTask>,
    val freeMinutes: Int,
    val energy: Energy,
    /** Free minutes left on a later day. */
    val roomOn: (LocalDate) -> Int,
)

data class Pick(val mission: Mission, val reason: Reason, val isFixedBlock: Boolean)

data class DidntFit(val missionId: Long, val due: LocalDate, val roomDay: LocalDate?)

data class PlanResult(val picks: List<Pick>, val didntFit: DidntFit?)

/** Spec section 6.2. */
object Planner {

    const val LOW_ENERGY_SHARE = 0.6
    const val HEAVY_DAY_MINUTES = 6 * 60

    private data class Candidate(
        val mission: Mission,
        /**
         * Lower is more urgent: 2 overdue, 3 streak at risk, 4 quota due, preferred day or recharge on a heavy day,
         * 5 milestone soon, 6 the rest.
         */
        val tier: Int,
        /** How far behind the mission's pillar is this week, 0 to 1. Recharge uses -1 so it sorts last. */
        val gap: Double,
        val reason: Reason,
        /** Set for things with a due date; only these can show as "didn't fit". */
        val due: LocalDate?,
    )

    /** [exclude] holds missions that may not be picked, such as ones swapped out today; they still count for budget and gaps. */
    fun plan(input: PlanInput, exclude: Set<Long> = emptySet()): PlanResult {
        val fixed = fixedBlocks(input)
        val picks = mutableListOf<Pick>()
        val skipped = mutableListOf<Candidate>()
        var left = budget(input)
        for (candidate in candidates(input, fixed).filter { it.mission.missionId !in exclude }) {
            val mission = candidate.mission
            if (fits(mission, input.energy, left)) {
                picks += Pick(mission, candidate.reason, isFixedBlock = false)
                left -= mission.durationMinutes
            } else {
                skipped += candidate
            }
        }
        val weekEnd = WeekMath.weekEnd(input.date)
        val didntFit = skipped.firstNotNullOfOrNull { candidate ->
            candidate.due?.takeIf { it <= weekEnd }?.let { due ->
                DidntFit(candidate.mission.missionId, due, nextRoomDay(input, candidate.mission.durationMinutes))
            }
        }
        return PlanResult(fixed + picks, didntFit)
    }

    /** The best candidate for "Swap one": fits what's left of today, same pillar first, never one already swapped out. */
    fun nextCandidate(input: PlanInput, swappedOut: Mission, exclude: Set<Long>): Pick? {
        val left = budget(input)
        val fitting = candidates(input, fixedBlocks(input))
            .filter { it.mission.missionId !in exclude && fits(it.mission, input.energy, left) }
        val best = fitting.firstOrNull { it.mission.pillarType == swappedOut.pillarType } ?: fitting.firstOrNull()
        return best?.let { Pick(it.mission, it.reason, isFixedBlock = false) }
    }

    /**
     * Spec 6.3, the pencil: every live mission that isn't a fixed block and isn't on the day's list yet,
     * each with its reason line. Today's candidates come first, in pick order; the rest say where they stand.
     * It ignores the day's time budget, its energy and swapped-out missions on purpose: the list is for
     * adding by hand, so the owner decides what fits. Paused and not-yet-started missions aren't live, so they're left out.
     */
    fun options(input: PlanInput): List<Pick> {
        val ranked = candidates(input, fixedBlocks(input)).map { Pick(it.mission, it.reason, isFixedBlock = false) }
        val rankedIds = ranked.map { it.mission.missionId }.toSet()
        val rest = input.missions
            .filter { it.missionKind != MissionKind.BLOCK && it.isLiveOn(input.date) && it.missionId !in rankedIds }
            .filter { mission -> input.todayTasks.none { it.missionId == mission.missionId } }
            .map { Pick(it, standing(it, input), isFixedBlock = false) }
        return ranked + rest
    }

    /** The reason line for a mission that isn't a candidate today: this week's count, or the last time it was done. */
    private fun standing(mission: Mission, input: PlanInput): Reason {
        val date = input.date
        val history = input.history
        return when {
            mission.missionKind == MissionKind.RECHARGE -> Reason(ReasonCode.RECHARGE_USUAL, listOf(mission.count.toString()))
            mission.repeatsByInterval -> {
                val last = history.lastDoneBefore(mission.missionId, date)
                if (last == null) {
                    // Not reached from options() today: a live rhythm never done is due from its start date, so it's a candidate.
                    Reason(ReasonCode.RHYTHM_FIRST, listOf(mission.intervalLabel()))
                } else {
                    Reason(ReasonCode.RHYTHM_DUE, listOf(last.daysUntil(date).toString(), mission.intervalLabel()))
                }
            }
            // Not reached from options() today: a daily habit is always a candidate. Kept so the reason stays right if that changes.
            mission.periodUnit == Period.DAY ->
                Reason(ReasonCode.STREAK, listOf(history.streakBefore(mission.missionId, date).toString()))
            else -> {
                val weekStart = WeekMath.weekStart(date)
                val weekEnd = WeekMath.weekEnd(date)
                val target = Points.occurrencesInWeek(mission, weekStart, history)
                val done = history.doneCount(mission.missionId, weekStart, date.minus(1, DateTimeUnit.DAY))
                val lastDay = mission.endDate?.toLocalDate()?.let { minOf(it, weekEnd) } ?: weekEnd
                Reason(ReasonCode.QUOTA, listOf(done.toString(), target.toString(), (date.daysUntil(lastDay) + 1).toString()))
            }
        }
    }

    /** Free minutes the flexible picks may use, after anything already on today's list. */
    fun budget(input: PlanInput): Int {
        val share = if (input.energy == Energy.LOW) LOW_ENERGY_SHARE else 1.0
        val minutesById = input.missions.associate { it.missionId to it.durationMinutes }
        val alreadyPlanned = input.todayTasks
            .filter { !it.isFixedBlock && it.taskState.isOpen }
            .sumOf { minutesById[it.missionId] ?: 0 }
        return (input.freeMinutes * share).toInt() - alreadyPlanned
    }

    private fun fits(mission: Mission, energy: Energy, minutesLeft: Int): Boolean =
        !(energy == Energy.LOW && mission.effortLevel == Effort.DRAINING) && mission.durationMinutes <= minutesLeft

    private fun fixedBlocks(input: PlanInput): List<Pick> = input.missions
        .filter { it.missionKind == MissionKind.BLOCK && it.isLiveOn(input.date) && it.isScheduledOn(input.date) }
        .filter { mission -> input.todayTasks.none { it.missionId == mission.missionId } }
        .map { Pick(it, Reason(ReasonCode.BLOCK, listOf(it.startTime.orEmpty(), it.endTime.orEmpty())), isFixedBlock = true) }

    private fun candidates(input: PlanInput, fixed: List<Pick>): List<Candidate> {
        val weekStart = WeekMath.weekStart(input.date)
        val weekEnd = WeekMath.weekEnd(input.date)
        val minutesById = input.missions.associate { it.missionId to it.durationMinutes }
        val fixedMinutesToday = fixed.sumOf { it.mission.durationMinutes } +
            input.todayTasks.filter { it.isFixedBlock }.sumOf { minutesById[it.missionId] ?: 0 }
        val gaps = PillarType.scoredPillars.associateWith { pillarGap(input, it, weekStart) }
        return input.missions
            .filter { it.missionKind != MissionKind.BLOCK && it.isLiveOn(input.date) }
            .filter { mission -> input.todayTasks.none { it.missionId == mission.missionId } }
            .mapNotNull { candidateFor(it, input, weekStart, weekEnd, fixedMinutesToday, gaps[it.pillarType] ?: 0.0) }
            .sortedWith(
                compareBy<Candidate> { it.tier }
                    .thenByDescending { it.gap }
                    .thenBy(nullsLast<LocalDate>()) { it.due }
                    .thenByDescending { it.mission.weight }
                    .thenBy { it.mission.missionId }
            )
    }

    private fun candidateFor(
        mission: Mission,
        input: PlanInput,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        fixedMinutesToday: Int,
        gap: Double,
    ): Candidate? {
        val date = input.date
        val history = input.history
        val yesterday = date.minus(1, DateTimeUnit.DAY)

        if (mission.missionKind == MissionKind.RECHARGE) {
            val done = history.doneCount(mission.missionId, weekStart, yesterday)
            return when {
                fixedMinutesToday >= HEAVY_DAY_MINUTES -> Candidate(
                    mission, 4, -1.0, Reason(ReasonCode.RECHARGE_HEAVY, listOf((fixedMinutesToday / 60).toString())), null
                )
                done < mission.count -> Candidate(
                    mission, 6, -1.0, Reason(ReasonCode.RECHARGE_USUAL, listOf(mission.count.toString())), null
                )
                else -> null
            }
        }

        if (mission.repeatsByInterval) {
            val last = history.lastDoneBefore(mission.missionId, date)
            val due = last?.plus(mission.intervalDays(), DateTimeUnit.DAY) ?: mission.startDate.toLocalDate()
            // Weekly or slower may be done early in the week; shorter intervals wait until they're due.
            val horizon = if (mission.intervalDays() < Period.WEEK.days) date else weekEnd
            if (due > horizon) return null
            val label = mission.intervalLabel()
            return when {
                due < date -> Candidate(mission, 2, gap, Reason(ReasonCode.RHYTHM_OVERDUE, listOf(due.daysUntil(date).toString())), due)
                last == null -> Candidate(mission, 6, gap, Reason(ReasonCode.RHYTHM_FIRST, listOf(label)), due)
                else -> Candidate(mission, 6, gap, Reason(ReasonCode.RHYTHM_DUE, listOf(last.daysUntil(date).toString(), label)), due)
            }
        }

        if (mission.periodUnit == Period.DAY) {
            val streak = history.streakBefore(mission.missionId, date)
            return Candidate(mission, if (streak > 0) 3 else 6, gap, Reason(ReasonCode.STREAK, listOf(streak.toString())), null)
        }

        // Weekly count: habits and finish lines. Target and days left follow the mission's live days.
        val target = Points.occurrencesInWeek(mission, weekStart, history)
        val done = history.doneCount(mission.missionId, weekStart, yesterday)
        val remaining = target - done
        if (remaining <= 0) return null
        val lastDay = mission.endDate?.toLocalDate()?.let { minOf(it, weekEnd) } ?: weekEnd
        val daysLeft = date.daysUntil(lastDay) + 1
        val urgent = remaining >= daysLeft
        val preferred = mission.isScheduledOn(date)
        // Hold a habit for its preferred days still ahead this week, unless it's needed today.
        val preferredAhead = (1 until daysLeft).count { mission.isScheduledOn(date.plus(it, DateTimeUnit.DAY)) }
        if (!urgent && !preferred && remaining <= preferredAhead) return null
        // Spread the week out: not two days in a row unless it's needed or preferred. Each week starts fresh.
        val pickedYesterday = yesterday >= weekStart && history.pickedOn(mission.missionId, yesterday)
        if (!urgent && !preferred && pickedYesterday) return null
        val milestone = nextMilestone(mission, input.milestones, date)
        val tier = when {
            urgent || preferred -> 4
            milestone != null -> 5
            else -> 6
        }
        val reason = milestone?.first
            ?: Reason(ReasonCode.QUOTA, listOf(done.toString(), target.toString(), daysLeft.toString()))
        return Candidate(mission, tier, gap, reason, milestone?.second)
    }

    /** The next open milestone if it's due within 7 days, with its reason and date. */
    private fun nextMilestone(mission: Mission, milestones: List<Milestone>, date: LocalDate): Pair<Reason, LocalDate>? {
        val mine = milestones.filter { it.missionId == mission.missionId }.sortedBy { it.milestoneOrder }
        val nextIndex = mine.indexOfFirst { it.status != TaskStatus.COMPLETED.value }
        if (nextIndex < 0) return null
        val dueDate = mine[nextIndex].expectedCompletionDate.toLocalDate()
        if (date.daysUntil(dueDate) > 7) return null
        val reason = Reason(ReasonCode.MILESTONE, listOf((nextIndex + 1).toString(), mine.size.toString(), dueDate.shortLabel()))
        return reason to dueDate
    }

    private fun pillarGap(input: PlanInput, pillar: PillarType, weekStart: LocalDate): Double {
        val target = Points.weeklyTarget(input.missions, pillar, weekStart, input.history)
        if (target == 0) return 0.0
        val earned = Points.earnedInWeek(input.earnedMissions, input.history.all, pillar, weekStart)
        return (1.0 - earned.toDouble() / target).coerceAtLeast(0.0)
    }

    private fun nextRoomDay(input: PlanInput, minutes: Int): LocalDate? =
        (1..7).map { input.date.plus(it, DateTimeUnit.DAY) }.firstOrNull { input.roomOn(it) >= minutes }
}
