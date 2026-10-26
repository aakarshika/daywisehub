package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** One pillar's points on one day. */
data class DaySlice(val pillar: PillarType, val points: Int)

/** One day of the month calendar. */
data class CalendarDay(
    val date: LocalDate,
    /** False for the days of the first and last rows that belong to the next or previous month. */
    val inMonth: Boolean,
    val isToday: Boolean,
    /**
     * False after today, and in a finished week the diary wasn't opened in (which takes in every week
     * before the first item): such a day has no ring, so it doesn't look like a day with nothing done.
     */
    val hasRing: Boolean,
    /** Points earned per pillar that day, in pillar order, without empty ones. */
    val slices: List<DaySlice>,
    /**
     * Points that didn't happen: skipped or moved items, the missing half of a did-some, and anything still
     * planned on an earlier day. Today's planned items can still happen, so they aren't counted.
     */
    val notDone: Int,
    /** The day's badges, rarest first (spec 9.3). The cell shows the first; tapping the day lists them all. */
    val badges: List<EarnedBadge>,
    /** "Tuesday, Oct 20", with the year in another year than this week's: the badge list's title. */
    val title: String,
    /** What a screen reader says for the day. */
    val description: String,
)

data class CalendarWeek(
    val weekStart: LocalDate,
    val days: List<CalendarDay>,
    val crowned: Boolean,
    /** What a screen reader says for the crown: "Balanced week of Oct 12". */
    val crownDescription: String,
    val selected: Boolean,
)

data class MonthCalendar(
    /** "OCTOBER 2026" */
    val title: String,
    val weeks: List<CalendarWeek>,
)

/** Spec 9: the Monday-first month calendar on the Metrics tab. */
object MonthGrid {

    /** The days the month of [week]'s Monday shows: from the Monday of its first week to the Sunday of its last. */
    fun span(week: LocalDate): ClosedRange<LocalDate> {
        val first = LocalDate(week.year, week.month, 1)
        val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        return WeekMath.weekStart(first)..WeekMath.weekEnd(last)
    }

    /**
     * The month of [week]'s Monday, as rows of whole weeks from Monday to Sunday. [allMissions] include
     * archived ones, so every tick keeps its pillar. Only finished weeks get a crown, and a finished week
     * that isn't in [opened] (PaceOffers.openedWeeks) gets no rings, though its days keep their badges.
     * Each task is read once, through [days]; [badges] covers [span].
     */
    fun build(
        week: LocalDate,
        today: LocalDate,
        allMissions: List<Mission>,
        days: DayTasks,
        byWeek: Map<LocalDate, WeekSummary>,
        opened: Set<LocalDate>,
        badges: DayBadgeBook,
    ): MonthCalendar {
        val first = LocalDate(week.year, week.month, 1)
        val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        val thisWeek = WeekMath.weekStart(today)
        val byId = allMissions.associateBy { it.missionId }
        val weeks = mutableListOf<CalendarWeek>()
        var monday = WeekMath.weekStart(first)
        while (monday <= last) {
            val summary = byWeek[monday]
            val finished = monday < thisWeek
            val away = finished && WeekScores.isAway(monday, opened)
            weeks += CalendarWeek(
                weekStart = monday,
                days = (0..6).map {
                    val date = monday.plus(it, DateTimeUnit.DAY)
                    day(date, first.month, today, byId, days, ringed = !away, badges.on(date))
                },
                crowned = finished && summary?.balanced == true,
                crownDescription = "Balanced week of ${monday.shortLabel()}",
                selected = monday == week,
            )
            monday = monday.plus(7, DateTimeUnit.DAY)
        }
        return MonthCalendar("${first.month.name} ${first.year}", weeks)
    }

    private fun day(
        date: LocalDate,
        month: Month,
        today: LocalDate,
        byId: Map<Long, Mission>,
        days: DayTasks,
        ringed: Boolean,
        badges: List<EarnedBadge>,
    ): CalendarDay {
        val earned = mutableMapOf<PillarType, Int>()
        var notDone = 0
        if (date <= today) {
            for (task in days.on(date)) {
                val mission = byId[task.missionId] ?: continue
                // Recharge never scores, so it has no slice.
                if (!mission.scored) continue
                val pillar = mission.pillarType
                when (task.taskState) {
                    TaskState.DONE -> earned[pillar] = (earned[pillar] ?: 0) + task.pointsEarned
                    TaskState.PARTIAL -> {
                        earned[pillar] = (earned[pillar] ?: 0) + task.pointsEarned
                        notDone += (mission.weight - task.pointsEarned).coerceAtLeast(0)
                    }
                    TaskState.SKIPPED, TaskState.MOVED -> notDone += mission.weight
                    TaskState.PLANNED -> if (date < today) notDone += mission.weight
                }
            }
        }
        val slices = PillarType.scoredPillars.mapNotNull { pillar -> earned[pillar]?.takeIf { it > 0 }?.let { DaySlice(pillar, it) } }
        return CalendarDay(
            date = date,
            inMonth = date.month == month,
            isToday = date == today,
            hasRing = ringed && date <= today,
            slices = slices,
            notDone = notDone,
            badges = badges,
            title = "${date.weekdayName()}, ${date.labelFrom(WeekMath.weekStart(today))}",
            description = describe(date, today, slices, notDone, badges),
        )
    }

    /** "Monday, October 26. 10 points: Health 2, Wealth 6, Love 2. 3 points didn't happen. Badges: Hat trick, First one" */
    private fun describe(date: LocalDate, today: LocalDate, slices: List<DaySlice>, notDone: Int, badges: List<EarnedBadge>): String {
        val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val name = "${date.weekdayName()}, $month ${date.dayOfMonth}"
        val parts = mutableListOf(if (date == today) "Today, $name" else name)
        if (date <= today) {
            val total = slices.sumOf { it.points }
            parts += when {
                total > 0 -> "${MissionWords.pointsPhrase(total)}: " + slices.joinToString(", ") { "${it.pillar.title} ${it.points}" }
                // Today can still earn some.
                date == today -> "No points yet"
                else -> "No points"
            }
            if (notDone > 0) parts += "${MissionWords.pointsPhrase(notDone)} didn't happen"
        }
        if (badges.isNotEmpty()) parts += (if (badges.size == 1) "Badge: " else "Badges: ") + badges.joinToString(", ") { it.badge.title }
        return parts.joinToString(". ")
    }
}
