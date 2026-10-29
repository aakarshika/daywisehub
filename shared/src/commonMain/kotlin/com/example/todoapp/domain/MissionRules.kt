package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

val Mission.pillarType: PillarType get() = PillarType.valueOf(pillar)
val Mission.missionKind: MissionKind get() = MissionKind.valueOf(kind)
val Mission.effortLevel: Effort get() = Effort.valueOf(effort)
val Mission.ifMissedRule: IfMissed get() = IfMissed.valueOf(ifMissed)
val Mission.periodUnit: Period get() = Period.valueOf(period)
val Mission.intervalPeriod: Period get() = Period.valueOf(intervalUnit)
val Mission.scored: Boolean get() = pillarType.scored

/**
 * The diary and Metrics hide a pillar with no active mission, unless the week shown has points due or
 * earned in it (a mission archived mid-week keeps its pillar there).
 */
fun PillarType.shows(missions: List<Mission>, earned: Int, target: Int): Boolean =
    earned > 0 || target > 0 || missions.any { it.active && it.pillarType == this }

/** ISO weekday numbers: fixed days for blocks, preferred days for habits. */
val Mission.daySet: Set<Int> get() = weekDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

/** Rhythms, and habits counted per month, repeat on an interval instead of a weekly count. */
val Mission.repeatsByInterval: Boolean
    get() = missionKind == MissionKind.RHYTHM ||
        (missionKind != MissionKind.BLOCK && periodUnit == Period.MONTH)

fun Mission.intervalDays(): Int = when {
    missionKind == MissionKind.RHYTHM -> intervalCount.coerceAtLeast(1) * intervalPeriod.days
    periodUnit == Period.MONTH -> (Period.MONTH.days / count.coerceAtLeast(1)).coerceAtLeast(1)
    else -> Period.WEEK.days
}

/** "week", "2 weeks", "3 months"; "15 days" for a twice-a-month habit. */
fun Mission.intervalLabel(): String {
    if (missionKind != MissionKind.RHYTHM) return "${intervalDays()} days"
    val unit = intervalPeriod.name.lowercase()
    return if (intervalCount <= 1) unit else "$intervalCount ${unit}s"
}

/** Started, not ended, not paused, not archived. */
fun Mission.isLiveOn(date: LocalDate): Boolean {
    if (!active) return false
    if (date < startDate.toLocalDate()) return false
    endDate?.let { if (date > it.toLocalDate()) return false }
    pausedUntil?.let { if (date < it.toLocalDate()) return false }
    return true
}

fun Mission.isScheduledOn(date: LocalDate): Boolean = date.dayOfWeek.isoDayNumber in daySet
