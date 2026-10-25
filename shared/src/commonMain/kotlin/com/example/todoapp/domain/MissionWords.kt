package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.models.TaskStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * Words for the mission sentence, the Missions screen and the week cost.
 * Everything here is built from stored fields only (spec 7.1): a title is never read for meaning.
 */
object MissionWords {

    /** Step 5's "a full day". */
    const val FULL_DAY_MINUTES = 480

    private val shortDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    /** ISO day number (Monday = 1) → "Mon". */
    fun shortDay(isoDay: Int): String = shortDays.getOrElse(isoDay - 1) { "" }

    /** "Mon to Fri", "Sat and Sun", "Mon, Wed and Fri", "Tue", "Every day". */
    fun daysLabel(days: Set<Int>): String {
        val sorted = days.filter { it in 1..7 }.sorted()
        return when {
            sorted.isEmpty() -> ""
            sorted.size == 7 -> "Every day"
            sorted.size == 1 -> shortDay(sorted.first())
            sorted.size >= 3 && sorted.last() - sorted.first() == sorted.size - 1 ->
                "${shortDay(sorted.first())} to ${shortDay(sorted.last())}"
            else -> sorted.dropLast(1).joinToString(", ") { shortDay(it) } + " and " + shortDay(sorted.last())
        }
    }

    fun weightWord(weight: Int): String = when {
        weight <= 1 -> "small"
        weight == 2 -> "solid"
        else -> "big"
    }

    fun effortWord(effort: Effort): String = when (effort) {
        Effort.LIGHT -> "light"
        Effort.STEADY -> "steady"
        Effort.DRAINING -> "draining"
    }

    fun ifMissedWords(rule: IfMissed): String = when (rule) {
        IfMissed.MOVE -> "move it"
        IfMissed.SAME_DAY -> "it has to be that day"
        IfMissed.SKIP -> "skip it"
    }

    /** The highlighted answer to step 3. */
    fun kindPhrase(kind: MissionKind): String = when (kind) {
        MissionKind.HABIT -> "something I keep doing"
        MissionKind.FINISH_LINE -> "something with a finish line"
        MissionKind.BLOCK -> "a fixed block in my day"
        MissionKind.RHYTHM -> "something I do every so often"
        MissionKind.RECHARGE -> "something that recharges me"
    }

    /** "30 min", "1½ h", "a full day". */
    fun durationWords(minutes: Int): String =
        if (minutes >= FULL_DAY_MINUTES) "a full day" else minutesLabel(minutes)

    /** "once", "twice", "3 times". */
    fun times(count: Int): String = when (count) {
        1 -> "once"
        2 -> "twice"
        else -> "$count times"
    }

    /** "about 3 times a week", "every day", "about twice a month". */
    fun countPhrase(count: Int, period: Period): String = when (period) {
        Period.DAY -> "every day"
        Period.WEEK -> "about ${times(count)} a week"
        Period.MONTH -> "about ${times(count)} a month"
    }

    /** "every week", "every 2 weeks", "every 3 months", "every 4 days". */
    fun intervalPhrase(count: Int, unit: Period): String {
        val name = unit.name.lowercase()
        return if (count <= 1) "every $name" else "every $count ${name}s"
    }

    /** "1 point", "6 points". */
    fun pointsPhrase(points: Int): String = if (points == 1) "1 point" else "$points points"

    /** 45 → "45 min", 60 → "1 hour", 90 → "1½ hours", 135 → "2 h 15 min". */
    fun hoursPhrase(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes == 60 -> "1 hour"
        minutes % 60 == 0 -> "${minutes / 60} hours"
        minutes % 60 == 30 -> "${minutes / 60}½ hours"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }

    /** Whole hours with a half: 1770 → "29½", 15 → "0", 30 → "½". */
    fun hoursNumber(minutes: Int): String {
        val whole = minutes.coerceAtLeast(0) / 60
        val half = minutes.coerceAtLeast(0) % 60 >= 30
        return when {
            whole == 0 && half -> "½"
            half -> "$whole½"
            else -> "$whole"
        }
    }

    /** The line under the free-hours bar. */
    fun spareLine(freeMinutes: Int, plannedMinutes: Int): String {
        val spare = freeMinutes - plannedMinutes
        return when {
            spare >= 5 * 60 -> "${hoursPhrase(spare)} to spare. Plenty of room."
            spare > 0 -> "${hoursPhrase(spare)} to spare."
            spare == 0 -> "That fills your free time exactly."
            else -> "${hoursPhrase(-spare)} more than your free time. Some of it will wait for days with room."
        }
    }

    /** Minutes after midnight → "9:00", "13:30". */
    fun clockLabel(minutes: Int): String = "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"

    /** "9:00", "09:00" or "13:30" → minutes after midnight; null when unreadable. */
    fun parseClock(text: String?): Int? {
        val parts = text?.trim()?.split(":") ?: return null
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].take(2).toIntOrNull() ?: return null
        if (hours !in 0..24 || minutes !in 0..59) return null
        return hours * 60 + minutes
    }

    /** The pace line on a mission card: "3× a week, on Mon · big", "Every 2 weeks · solid". */
    fun cadence(mission: Mission): String {
        val weight = weightWord(mission.weight)
        return when (mission.missionKind) {
            MissionKind.BLOCK -> {
                val days = daysLabel(mission.daySet)
                val start = mission.startTime.orEmpty()
                val end = mission.endTime.orEmpty()
                val window = if (start.isBlank() || end.isBlank()) "" else ", $start to $end"
                "$days$window · $weight"
            }
            MissionKind.RECHARGE -> "About ${times(mission.count)} a week"
            MissionKind.RHYTHM ->
                intervalPhrase(mission.intervalCount, mission.intervalPeriod).replaceFirstChar { it.uppercase() } + " · $weight"
            else -> {
                val pace = when (mission.periodUnit) {
                    Period.DAY -> "Every day"
                    Period.WEEK -> if (mission.count == 1) "Once a week" else "${mission.count}× a week"
                    Period.MONTH -> if (mission.count == 1) "Once a month" else "${mission.count}× a month"
                }
                val goal = mission.goalCount?.takeIf { it > mission.count }?.let { "up to ${it}×" }
                val days = mission.daySet.takeIf { it.isNotEmpty() && mission.periodUnit == Period.WEEK }
                    ?.let { "on ${daysLabel(it)}" }
                listOfNotNull(pace, goal, days).joinToString(", ") + " · $weight"
            }
        }
    }

    /** The status line on a mission card, from stored data only. */
    fun status(mission: Mission, history: History, milestones: List<Milestone>, today: LocalDate): String {
        mission.pausedUntil?.toLocalDate()?.let { if (it > today) return "Paused until ${it.shortLabel()}" }
        val start = mission.startDate.toLocalDate()
        if (start > today) return "Starts ${start.shortLabel()}"
        mission.endDate?.toLocalDate()?.let { if (it < today) return "Ended ${it.shortLabel()}" }
        val kind = mission.missionKind
        if (kind == MissionKind.BLOCK) return "Fixed block"
        if (kind == MissionKind.RECHARGE) return "Never counts as missed"
        if (mission.repeatsByInterval) {
            val last = history.lastDoneBefore(mission.missionId, today.plus(1, DateTimeUnit.DAY))
            val due = last?.plus(mission.intervalDays(), DateTimeUnit.DAY) ?: start
            val days = today.daysUntil(due)
            return when {
                days < -1 -> "Due ${-days} days ago"
                days == -1 -> "Due yesterday"
                days == 0 -> "Due today"
                due <= WeekMath.weekEnd(today) -> "Due by ${due.weekdayName()}"
                else -> "Next around ${due.shortLabel()}"
            }
        }
        if (mission.periodUnit == Period.DAY) {
            val doneToday = history.doneCount(mission.missionId, today, today) > 0
            val streak = history.streakBefore(mission.missionId, today) + if (doneToday) 1 else 0
            return if (streak > 0) "$streak-day streak" else "No streak yet"
        }
        if (kind == MissionKind.FINISH_LINE) {
            val mine = milestones.filter { it.missionId == mission.missionId }.sortedBy { it.milestoneOrder }
            if (mine.isNotEmpty()) {
                val next = mine.indexOfFirst { it.status != TaskStatus.COMPLETED.value }
                return if (next < 0) "All ${mine.size} milestones done" else "Milestone ${next + 1} of ${mine.size}"
            }
        }
        val weekStart = WeekMath.weekStart(today)
        val target = Points.occurrencesInWeek(mission, weekStart, history)
        val done = history.doneCount(mission.missionId, weekStart, WeekMath.weekEnd(today))
        return if (target > 0 && done >= target) "Done for this week" else "$done of $target this week"
    }
}
