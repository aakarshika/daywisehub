package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DaySettings
import kotlinx.datetime.LocalDate

/** One pillar's target this week, without and with the mission being saved. */
data class PillarCost(val pillar: PillarType, val before: Int, val after: Int)

/** Spec 4.2: what a mission costs this week, shown before it's saved. */
data class WeekCost(
    val pillars: List<PillarCost>,
    /** Free minutes in the week, from the per-weekday settings. */
    val freeMinutes: Int,
    /** Free minutes this week's flexible missions use, without the draft. */
    val plannedBefore: Int,
    /** The same with the draft (replacing its old version when editing). */
    val plannedAfter: Int,
    /** "Adds 1 hour and 6 points a week while you start gently. …" */
    val line: String,
)

/** This week's totals across a set of missions. */
object WeekLoad {

    fun freeMinutes(settings: List<DaySettings>): Int =
        (1..7).sumOf { day -> settings.firstOrNull { it.weekday == day }?.freeMinutes ?: DaySettings.defaultFor(day) }

    /** Fixed blocks use their own hours, so only the rest counts against free time. Recharge counts. */
    fun plannedMinutes(missions: List<Mission>, weekStart: LocalDate, history: History): Int =
        missions.filter { it.missionKind != MissionKind.BLOCK }
            .sumOf { it.durationMinutes * Points.occurrencesInWeek(it, weekStart, history) }

    fun points(missions: List<Mission>, weekStart: LocalDate, history: History): Int =
        PillarType.scoredPillars.sumOf { Points.weeklyTarget(missions, it, weekStart, history) }
}

object WeekCosts {

    /** [missions] are the active missions as stored; an edited mission's old version is among them. */
    fun compute(
        draft: MissionDraft,
        missions: List<Mission>,
        history: History,
        settings: List<DaySettings>,
        today: LocalDate,
    ): WeekCost {
        val weekStart = WeekMath.weekStart(today)
        val mission = draft.toMission()
        val isNew = mission.missionId == 0L
        val after = missions.filter { isNew || it.missionId != mission.missionId } + mission
        return WeekCost(
            pillars = PillarType.scoredPillars.map { pillar ->
                PillarCost(
                    pillar,
                    before = Points.weeklyTarget(missions, pillar, weekStart, history),
                    after = Points.weeklyTarget(after, pillar, weekStart, history),
                )
            },
            freeMinutes = WeekLoad.freeMinutes(settings),
            plannedBefore = WeekLoad.plannedMinutes(missions, weekStart, history),
            plannedAfter = WeekLoad.plannedMinutes(after, weekStart, history),
            line = line(draft, Points.occurrencesInWeek(mission, weekStart, history), isNew),
        )
    }

    /** The plain-words line, about a usual week, plus a note when this week counts fewer times. */
    fun line(draft: MissionDraft, thisWeek: Int, isNew: Boolean): String {
        val mission = draft.toMission()
        val verb = if (isNew) "Adds" else "Comes to"
        val minutes = mission.durationMinutes
        val weight = mission.weight
        return when (draft.kind) {
            MissionKind.BLOCK -> {
                val days = draft.weekDays.size
                "$verb ${MissionWords.pointsPhrase(days * weight)} a week. Fixed blocks use their own hours, not your free time." +
                    weekNote(thisWeek, days)
            }
            MissionKind.RECHARGE ->
                "$verb about ${MissionWords.hoursPhrase(mission.count * minutes)} of free time a week. " +
                    "No points, and it never counts as missed." + weekNote(thisWeek, mission.count)
            MissionKind.RHYTHM ->
                "$verb ${MissionWords.hoursPhrase(minutes)} and ${MissionWords.pointsPhrase(weight)} each time, " +
                    "${MissionWords.intervalPhrase(draft.intervalCount, draft.intervalUnit)}." + dueNote(thisWeek)
            else -> when (draft.period) {
                Period.DAY ->
                    "$verb ${MissionWords.hoursPhrase(7 * minutes)} and ${MissionWords.pointsPhrase(7 * weight)} a week, one each day." +
                        weekNote(thisWeek, 7)
                Period.MONTH ->
                    "$verb ${MissionWords.hoursPhrase(minutes)} and ${MissionWords.pointsPhrase(weight)} each time, " +
                        "${MissionWords.times(mission.count)} a month." + dueNote(thisWeek)
                Period.WEEK -> {
                    val now = mission.count
                    val base = "$verb ${MissionWords.hoursPhrase(now * minutes)} and ${MissionWords.pointsPhrase(now * weight)} a week"
                    val gentle = if (draft.gentleOn) {
                        " while you start gently. That's ${MissionWords.pointsPhrase(draft.count * weight)} once you're at ${draft.count}×."
                    } else "."
                    base + gentle + weekNote(thisWeek, now)
                }
            }
        }
    }

    private fun weekNote(thisWeek: Int, full: Int): String = when {
        thisWeek >= full -> ""
        thisWeek == 0 -> " It doesn't come up this week."
        else -> " This week it's $thisWeek×."
    }

    private fun dueNote(thisWeek: Int): String = if (thisWeek == 0) " It isn't due this week." else ""
}
