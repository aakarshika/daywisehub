package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/** The nine questions of spec section 4.1, in order. */
enum class EditorStep { TITLE, PILLAR, KIND, PACE, DURATION, EFFORT, WEIGHT, IF_MISSED, GENTLE }

/** One milestone line of a finish line. [milestoneId] is 0 until it's saved. */
data class MilestoneDraft(val milestoneId: Long = 0, val text: String = "")

/**
 * A mission being written or edited. [count] is the full pace; a gentle start saves a lower
 * count and keeps [count] as `goalCount`. Answers a kind doesn't use are ignored when saving.
 */
data class MissionDraft(
    val missionId: Long = 0,
    val title: String = "",
    /** Step 2. Kept while the kind is Recharge, so switching back restores it. */
    val pillarChoice: PillarType? = null,
    val kind: MissionKind? = null,
    val count: Int = 3,
    val period: Period = Period.WEEK,
    val gentle: Boolean = false,
    /** The current pace of a gentle start being edited; dropped when the goal changes. */
    val gentleCount: Int? = null,
    val intervalCount: Int = 2,
    val intervalUnit: Period = Period.WEEK,
    /** Fixed days for a block, optional preferred days for a weekly habit or finish line. */
    val weekDays: Set<Int> = emptySet(),
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 12 * 60,
    val endDate: LocalDate? = null,
    val milestones: List<MilestoneDraft> = emptyList(),
    val durationMinutes: Int = 30,
    val effort: Effort = Effort.STEADY,
    val weight: Int = 2,
    val ifMissed: IfMissed = IfMissed.MOVE,
    val startDate: LocalDate,
    val pausedUntil: MyDate? = null,
    val motivation: String? = null,
) {
    val pillar: PillarType
        get() = if (kind == MissionKind.RECHARGE) PillarType.RECHARGE else pillarChoice ?: PillarType.LIFE

    /** Step 9 is asked only for habits and finish lines with 2 or more sessions a week. */
    val gentleEligible: Boolean
        get() = (kind == MissionKind.HABIT || kind == MissionKind.FINISH_LINE) && period == Period.WEEK && count >= 2

    val gentleOn: Boolean get() = gentle && gentleEligible

    /** Where a gentle start begins: the pace kept from editing, or about half the goal. */
    val gentleStartCount: Int
        get() = gentleCount?.takeIf { it in 1 until count } ?: MissionDrafts.gentleStart(count)

    /** The pace the planner uses now: lower while starting gently. */
    val currentCount: Int
        get() = if (gentleOn) gentleStartCount else count

    /** Why this draft can't be saved yet, or null when it can. */
    val blocker: String?
        get() = when {
            title.isBlank() -> "Give it a name."
            kind == null -> "Pick what kind of thing it is."
            kind == MissionKind.BLOCK && weekDays.isEmpty() -> "Pick at least one day."
            kind == MissionKind.BLOCK && endMinute <= startMinute -> "It has to end after it starts."
            else -> null
        }

    /** Weekly habits and finish lines may have preferred days; blocks must have days. */
    val usesDays: Boolean
        get() = kind == MissionKind.BLOCK ||
            ((kind == MissionKind.HABIT || kind == MissionKind.FINISH_LINE) && period == Period.WEEK)

    /** Recharge skips the pillar, weight and sideways questions; a block takes its length from its times. */
    fun applies(step: EditorStep): Boolean = when (step) {
        EditorStep.PILLAR, EditorStep.WEIGHT -> kind != MissionKind.RECHARGE
        EditorStep.DURATION -> kind != MissionKind.BLOCK
        EditorStep.IF_MISSED -> kind != MissionKind.BLOCK && kind != MissionKind.RECHARGE
        EditorStep.GENTLE -> kind == null || gentleEligible
        else -> true
    }

    val steps: List<EditorStep> get() = EditorStep.entries.filter { applies(it) }

    /** Fresh pace answers for a newly picked kind. Picking the same kind changes nothing. */
    fun withKind(newKind: MissionKind, today: LocalDate): MissionDraft {
        if (newKind == kind) return this
        val base = copy(kind = newKind, gentle = false, gentleCount = null, weekDays = emptySet())
        return when (newKind) {
            MissionKind.HABIT -> base.copy(count = 3, period = Period.WEEK)
            MissionKind.FINISH_LINE -> base.copy(
                count = 2,
                period = Period.WEEK,
                endDate = endDate ?: maxOf(today, startDate).plus(MissionDrafts.DEFAULT_FINISH_WEEKS * 7, DateTimeUnit.DAY),
            )
            MissionKind.BLOCK -> base.copy(weekDays = setOf(1, 2, 3, 4, 5), startMinute = 9 * 60, endMinute = 12 * 60)
            MissionKind.RHYTHM -> base.copy(intervalCount = 2, intervalUnit = Period.WEEK)
            MissionKind.RECHARGE -> base.copy(count = 2, period = Period.WEEK)
        }
    }

    /** The mission this draft saves as. */
    fun toMission(): Mission {
        val k = kind ?: MissionKind.HABIT
        val counted = k == MissionKind.HABIT || k == MissionKind.FINISH_LINE || k == MissionKind.RECHARGE
        return Mission(
            missionId = missionId,
            title = title.trim(),
            pillar = pillar.name,
            kind = k.name,
            weight = if (k == MissionKind.RECHARGE) 0 else weight,
            durationMinutes = if (k == MissionKind.BLOCK) endMinute - startMinute else durationMinutes,
            effort = effort.name,
            ifMissed = when (k) {
                MissionKind.BLOCK -> IfMissed.SAME_DAY
                MissionKind.RECHARGE -> IfMissed.SKIP
                else -> ifMissed
            }.name,
            count = if (counted) currentCount else 1,
            // Only habits count per day or month; finish lines are weekly (spec 3).
            period = (if (k == MissionKind.HABIT) period else Period.WEEK).name,
            goalCount = if (gentleOn) count else null,
            intervalCount = if (k == MissionKind.RHYTHM) intervalCount else 0,
            intervalUnit = (if (k == MissionKind.RHYTHM) intervalUnit else Period.WEEK).name,
            weekDays = if (usesDays) weekDays.sorted().joinToString(",") else "",
            startTime = if (k == MissionKind.BLOCK) MissionWords.clockLabel(startMinute) else null,
            endTime = if (k == MissionKind.BLOCK) MissionWords.clockLabel(endMinute) else null,
            startDate = MyDate.fromLocalDate(startDate),
            endDate = if (k == MissionKind.FINISH_LINE) endDate?.let { MyDate.fromLocalDate(it) } else null,
            pausedUntil = pausedUntil,
            motivation = motivation,
        )
    }
}

object MissionDrafts {

    const val DEFAULT_FINISH_WEEKS = 8
    const val MAX_FINISH_WEEKS = 52
    const val MAX_MILESTONES = 5
    const val MAX_TITLE_LENGTH = 60
    const val MAX_MILESTONE_LENGTH = 80
    /** Blocks move in half hours. */
    const val CLOCK_STEP = 30
    const val DAY_MINUTES = 24 * 60

    /** Step 5's choices; the last is "a full day". */
    val durationChoices = listOf(10, 15, 20, 30, 45, 60, 90, 120, 180, 240, MissionWords.FULL_DAY_MINUTES)

    /**
     * "Every so often" stops at 3 months: the planner reads 120 days of history
     * (PlanRepository.HISTORY_DAYS), so a longer rhythm would look never done.
     */
    fun intervalRange(unit: Period): IntRange = when (unit) {
        Period.DAY -> 2..30
        Period.WEEK -> 1..12
        Period.MONTH -> 1..3
    }

    /** About half the goal, rounded up: 3 → 2, 5 → 3, 7 → 4. */
    fun gentleStart(goal: Int): Int = ((goal + 1) / 2).coerceAtLeast(1)

    fun new(today: LocalDate): MissionDraft = MissionDraft(startDate = today)

    /** A stored mission as a draft. Stored names that don't match an answer fall back to a default. */
    fun from(mission: Mission, milestones: List<Milestone>): MissionDraft {
        val kind = enumOr(mission.kind, MissionKind.HABIT)
        val period = enumOr(mission.period, Period.WEEK)
        // Finish lines are weekly; one stored as every day is 7 times a week.
        val dailyFinish = kind == MissionKind.FINISH_LINE && period == Period.DAY
        val goal = mission.goalCount?.takeIf { it > mission.count && !dailyFinish }
        val unit = enumOr(mission.intervalUnit, Period.WEEK)
        val start = (MissionWords.parseClock(mission.startTime) ?: (9 * 60)).coerceIn(0, DAY_MINUTES - CLOCK_STEP)
        var end = MissionWords.parseClock(mission.endTime) ?: (start + mission.durationMinutes)
        // Early sample blocks wrote afternoons without a 12-hour offset ("9:00 to 1:00").
        if (end <= start && end + 12 * 60 > start) end += 12 * 60
        return MissionDraft(
            missionId = mission.missionId,
            title = mission.title,
            pillarChoice = enumOr<PillarType>(mission.pillar, PillarType.LIFE).takeIf { it.scored },
            kind = kind,
            count = if (dailyFinish) 7 else goal ?: mission.count.coerceAtLeast(1),
            period = if (dailyFinish) Period.WEEK else period,
            gentle = goal != null,
            gentleCount = if (goal != null) mission.count else null,
            intervalCount = mission.intervalCount.coerceIn(intervalRange(unit)),
            intervalUnit = unit,
            weekDays = mission.weekDays.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet(),
            startMinute = start,
            endMinute = end.coerceIn(start + CLOCK_STEP, DAY_MINUTES),
            endDate = mission.endDate?.toLocalDate(),
            milestones = milestones.sortedBy { it.milestoneOrder }.map { MilestoneDraft(it.milestoneId, it.text) },
            durationMinutes = mission.durationMinutes.coerceAtLeast(5),
            effort = enumOr(mission.effort, Effort.STEADY),
            weight = mission.weight.coerceIn(1, 3),
            ifMissed = enumOr(mission.ifMissed, IfMissed.MOVE),
            startDate = mission.startDate.toLocalDate(),
            pausedUntil = mission.pausedUntil,
            motivation = mission.motivation,
        )
    }

    /** Due dates for [count] milestones, spread evenly from [start] to [end]; the last one is the end date. */
    fun milestoneDates(start: LocalDate, end: LocalDate?, count: Int): List<LocalDate> {
        val last = end ?: start.plus(DEFAULT_FINISH_WEEKS * 7, DateTimeUnit.DAY)
        val days = start.daysUntil(last).coerceAtLeast(0)
        return (1..count).map { i -> start.plus(days * i / count, DateTimeUnit.DAY) }
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
