package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.TaskStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.random.Random

/** One mission on one blueprint day. [done] is null on a day that isn't over yet. */
data class BlueprintItem(
    val missionId: Long,
    val title: String,
    val pillar: PillarType,
    val minutes: Int,
    /** The app picks its days: a weekly count with no days chosen, a monthly count, or a rhythm. */
    val flexible: Boolean,
    val done: Boolean?,
)

/** A milestone's flag: on the day it was reached, or on its expected day while it's still open. */
data class BlueprintFlag(val missionId: Long, val pillar: PillarType, val text: String, val reached: Boolean)

data class BlueprintDay(
    val date: LocalDate,
    val past: Boolean,
    val today: Boolean,
    val items: List<BlueprintItem>,
    val flags: List<BlueprintFlag>,
)

/** A flexible mission eased off by one in a week because [because] lands in its pillar on [on]. */
data class EaseOff(
    val missionId: Long,
    val title: String,
    val pillar: PillarType,
    val count: Int,
    val usual: Int,
    val because: String,
    val on: LocalDate,
)

data class BlueprintWeek(val start: LocalDate, val days: List<BlueprintDay>, val easeOffs: List<EaseOff>)

data class Blueprint(val today: LocalDate, val weeks: List<BlueprintWeek>, val missions: List<Mission>) {
    val thisWeek: LocalDate get() = WeekMath.weekStart(today)
}

/** Everything, one pillar, or one mission. */
data class BlueprintFilter(val pillar: PillarType? = null, val missionId: Long? = null) {
    fun keeps(item: BlueprintItem): Boolean = keeps(item.missionId, item.pillar)
    fun keeps(flag: BlueprintFlag): Boolean = keeps(flag.missionId, flag.pillar)
    fun keeps(easeOff: EaseOff): Boolean = keeps(easeOff.missionId, easeOff.pillar)
    private fun keeps(id: Long, p: PillarType): Boolean = when {
        missionId != null -> id == missionId
        pillar != null -> p == pillar
        else -> true
    }
}

/** Average minutes on one weekday from today on, by pillar, split into fixed and flexible. */
data class UsualDay(val fixed: Map<PillarType, Int>, val flexible: Map<PillarType, Int>) {
    val total: Int get() = fixed.values.sum() + flexible.values.sum()
}

/**
 * The year at a glance: [WEEKS_BACK] weeks of what got done, then the weeks ahead. Days through next week
 * come from the stored plan, as the diary shows them. Later weeks are a guess: fixed days stay put, and
 * flexible missions are shuffled each week into days with room, seeded by mission and week, so a week
 * looks the same each time it's opened. A weekly flexible mission of 2× or more usually eases off by one
 * in a week its pillar gets an extra: a monthly item or a rhythm landing that week.
 */
object Blueprints {

    const val WEEKS_BACK = 12
    const val WEEKS = 52
    private const val EASE_OFF_CHANCE = 0.75
    /** Items this long or longer go on a weekend when there's one to pick. */
    private const val LONG_MINUTES = 120

    fun firstWeek(today: LocalDate): LocalDate = WeekMath.weekStart(today).minus(WEEKS_BACK * 7, DateTimeUnit.DAY)

    fun lastDay(today: LocalDate): LocalDate = firstWeek(today).plus(WEEKS * 7 - 1, DateTimeUnit.DAY)

    /** Flexible: the app picks the days. Fixed: blocks, chosen days, every day. */
    fun isFlexible(mission: Mission): Boolean = when {
        mission.missionKind == MissionKind.BLOCK -> false
        mission.repeatsByInterval -> true
        mission.periodUnit == Period.DAY -> false
        else -> mission.daySet.isEmpty()
    }

    fun build(
        today: LocalDate,
        missions: List<Mission>,
        tasks: List<TodayTask>,
        milestones: List<Milestone>,
        settings: List<DaySettings>,
    ): Blueprint {
        val byId = missions.associateBy { it.missionId }
        val history = History(tasks)
        val storedThrough = Projection.lastDay(today)
        val hasStoredAhead = tasks.any { it.date >= today }
        val storedByDate = tasks.filter { it.taskState != TaskState.MOVED }.groupBy { it.date }
        val free = (1..7).associateWith { day ->
            settings.firstOrNull { it.weekday == day }?.freeMinutes ?: DaySettings.defaultFor(day)
        }
        val live = missions.filter { it.active }.sortedBy { it.missionId }

        // When each interval mission is next due, carried from week to week as the guess places it.
        val nextDue = live.filter { it.repeatsByInterval }.associate { mission ->
            val last = history.tasksFor(mission.missionId)
                .filter { it.taskState.countsAsDone || (it.date >= today && it.taskState.isOpen) }
                .maxOfOrNull { it.date }
            mission.missionId to (last?.plus(mission.intervalDays(), DateTimeUnit.DAY) ?: maxOf(mission.startDate.toLocalDate(), today))
        }.toMutableMap()

        val flags = milestones.mapNotNull { mile ->
            val mission = byId[mile.missionId] ?: return@mapNotNull null
            val reached = mile.status == TaskStatus.COMPLETED.value && mile.actualCompletionDate != null
            val date = (if (reached) mile.actualCompletionDate else mile.expectedCompletionDate)?.toLocalDate() ?: return@mapNotNull null
            date to BlueprintFlag(mission.missionId, mission.pillarType, mile.text, reached)
        }.groupBy({ it.first }, { it.second })

        val first = firstWeek(today)
        val weeks = (0 until WEEKS).map { w ->
            val start = first.plus(w * 7, DateTimeUnit.DAY)
            val dates = (0..6).map { start.plus(it, DateTimeUnit.DAY) }
            val items = dates.associateWith { mutableListOf<BlueprintItem>() }
            // Past days, and the stored plan through next week; the rest of the week is guessed.
            val guessed = dates.filter { it >= today && (!hasStoredAhead || it > storedThrough) }
            dates.filterNot { it in guessed }.forEach { date ->
                storedByDate[date].orEmpty().forEach { task ->
                    val mission = byId[task.missionId] ?: return@forEach
                    if (date >= today && !task.taskState.isOpen) return@forEach
                    val flexible = isFlexible(mission)
                    val done = when {
                        date < today -> task.taskState.countsAsDone
                        task.taskState.countsAsDone -> true
                        else -> null
                    }
                    items.getValue(date) += item(mission, flexible, done)
                }
            }
            val easeOffs = if (guessed.isEmpty()) emptyList() else guess(start, guessed, items, live, nextDue, free, history, today)
            BlueprintWeek(
                start = start,
                days = dates.map { date ->
                    BlueprintDay(
                        date = date,
                        past = date < today,
                        today = date == today,
                        items = items.getValue(date).sortedWith(compareBy({ it.pillar.ordinal }, { it.flexible }, { it.missionId })),
                        flags = flags[date].orEmpty(),
                    )
                },
                easeOffs = easeOffs,
            )
        }
        return Blueprint(today, weeks, missions)
    }

    /** Average minutes per weekday (Monday first) over the days from [Blueprint.today] on. */
    fun usualWeek(blueprint: Blueprint, filter: BlueprintFilter): List<UsualDay> {
        val days = blueprint.weeks.flatMap { it.days }.filter { !it.past }
        return (1..7).map { weekday ->
            val same = days.filter { it.date.dayOfWeek.isoDayNumber == weekday }
            val count = same.size.coerceAtLeast(1)
            val kept = same.flatMap { day -> day.items.filter(filter::keeps) }
            fun sum(flexible: Boolean) = kept.filter { it.flexible == flexible }
                .groupBy { it.pillar }.mapValues { (_, list) -> list.sumOf { it.minutes } / count }
                .filterValues { it > 0 }
            UsualDay(fixed = sum(false), flexible = sum(true))
        }
    }

    private fun item(mission: Mission, flexible: Boolean, done: Boolean?) = BlueprintItem(
        missionId = mission.missionId,
        title = mission.title,
        pillar = mission.pillarType,
        minutes = mission.durationMinutes,
        flexible = flexible,
        done = done,
    )

    /** Fills [days] of the week starting [start] and returns what it eased off. */
    private fun guess(
        start: LocalDate,
        days: List<LocalDate>,
        items: Map<LocalDate, MutableList<BlueprintItem>>,
        live: List<Mission>,
        nextDue: MutableMap<Long, LocalDate>,
        free: Map<Int, Int>,
        history: History,
        today: LocalDate,
    ): List<EaseOff> {
        val weekKey = start.toEpochDays().toLong()
        fun load(date: LocalDate) = items.getValue(date).sumOf { it.minutes }
        fun room(date: LocalDate) = (free.getValue(date.dayOfWeek.isoDayNumber) - load(date)).coerceAtLeast(10)
        fun pick(candidates: List<LocalDate>, random: Random, chosen: List<LocalDate> = emptyList()): LocalDate =
            candidates.maxBy { date ->
                var score = (0.3 + random.nextDouble()) * room(date)
                // Not two days in a row, unless there's nowhere else.
                if (chosen.any { kotlin.math.abs(it.toEpochDays() - date.toEpochDays()) == 1 }) score *= 0.3
                score
            }

        // Fixed: blocks, every-day habits, and habits whose chosen days all fit the count.
        live.forEach { mission ->
            if (isFlexible(mission)) return@forEach
            val chosenDays = mission.daySet
            if (mission.missionKind != MissionKind.BLOCK && mission.periodUnit == Period.WEEK && mission.count < chosenDays.size) return@forEach
            days.filter { mission.isLiveOn(it) }.forEach { date ->
                val on = mission.missionKind != MissionKind.BLOCK && mission.periodUnit == Period.DAY || mission.isScheduledOn(date)
                if (on) items.getValue(date) += item(mission, false, null)
            }
        }

        // Rhythms and monthly counts land near their due day; they're the week's extras for the ease-off.
        val extras = mutableMapOf<PillarType, Pair<String, LocalDate>>()
        val weekEnd = start.plus(6, DateTimeUnit.DAY)
        live.filter { it.repeatsByInterval }.forEach { mission ->
            val random = Random(mission.missionId * 733 + weekKey)
            var due = nextDue[mission.missionId] ?: return@forEach
            while (due <= weekEnd) {
                val near = days.filter { mission.isLiveOn(it) && it >= due.minus(3, DateTimeUnit.DAY) }
                if (near.isEmpty()) {
                    due = weekEnd.plus(1, DateTimeUnit.DAY)
                    break
                }
                val weekend = near.filter { it.dayOfWeek.isoDayNumber >= 6 }
                val candidates = if (mission.durationMinutes >= LONG_MINUTES && weekend.isNotEmpty()) weekend else near
                val date = pick(candidates, random)
                items.getValue(date) += item(mission, true, null)
                extras.getOrPut(mission.pillarType) { mission.title to date }
                due = date.plus(mission.intervalDays(), DateTimeUnit.DAY)
            }
            nextDue[mission.missionId] = due
        }

        // Weekly counts: shuffled into days with room, a little lighter in a week with an extra.
        val easeOffs = mutableListOf<EaseOff>()
        live.forEach { mission ->
            if (mission.missionKind == MissionKind.BLOCK || mission.repeatsByInterval || mission.periodUnit != Period.WEEK) return@forEach
            val chosenDays = mission.daySet
            val flexible = chosenDays.isEmpty()
            if (!flexible && mission.count >= chosenDays.size) return@forEach
            val open = days.filter { mission.isLiveOn(it) && (flexible || it.dayOfWeek.isoDayNumber in chosenDays) }
            if (open.isEmpty()) return@forEach
            val random = Random(mission.missionId * 7919 + weekKey * 31)
            // A week already under way counts what's done in it.
            val doneSoFar = if (start < today) history.doneCount(mission.missionId, start, today.minus(1, DateTimeUnit.DAY)) else 0
            val usual = (mission.count - doneSoFar).coerceIn(0, open.size)
            var count = usual
            val extra = extras[mission.pillarType]
            if (flexible && extra != null && count >= 2 && random.nextDouble() < EASE_OFF_CHANCE) {
                count--
                easeOffs += EaseOff(mission.missionId, mission.title, mission.pillarType, count, usual, extra.first, extra.second)
            }
            val chosen = mutableListOf<LocalDate>()
            repeat(count) {
                val date = pick(open.filterNot { it in chosen }, random, chosen)
                chosen += date
                items.getValue(date) += item(mission, flexible, null)
            }
        }
        return easeOffs
    }
}
