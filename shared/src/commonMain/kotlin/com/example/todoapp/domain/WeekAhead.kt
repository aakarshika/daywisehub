package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * One line under "what next week adjusts" or the diary's Last week card. Only an [adjusted] line reports
 * something the app adjusted (a move, or a mission that stayed due), so only it gets the wand (spec 9.1).
 */
data class AheadLine(val parts: List<LinePart>, val adjusted: Boolean)

/** Something the app carried out of a week into the week after it, from stored data only. */
sealed class Carried(val mission: Mission) {
    /** A "move it" item that landed on [day], in the week after (spec 6.4). */
    class Moved(mission: Mission, val day: LocalDate) : Carried(mission)

    /** An every-so-often mission due in the week and not done, so it stays due (spec 5.1). */
    class StillDue(mission: Mission) : Carried(mission)
}

/**
 * "What next week adjusts" on the Metrics tab (spec 9), from stored data only. Each line names its
 * mission on a chip (spec 7.1), and the words around the chip never depend on the title. Nothing here
 * guesses why a week went the way it did: there's no "Love was light, so…".
 */
object WeekAhead {

    /** One cap for the diary's Last week card and the Metrics tab. */
    const val MAX_LINES = 3

    /**
     * What the app carried out of the week starting [week] into the week after it, one per mission and at
     * most [MAX_LINES]: "move it" items that landed in the week after, then every-so-often missions (short
     * rhythms too) due in [week] and not done. [missions] are the active ones, so an archived mission never
     * shows. The diary's Last week card and the Metrics tab both read this, so they never disagree about a
     * week; only their words differ.
     */
    fun carried(week: LocalDate, missions: List<Mission>, history: History): List<Carried> {
        val weekEnd = week.plus(6, DateTimeUnit.DAY)
        val next = week.plus(7, DateTimeUnit.DAY)
        val nextEnd = week.plus(13, DateTimeUnit.DAY)
        val byId = missions.associateBy { it.missionId }
        val named = mutableSetOf<Long>()
        val carried = mutableListOf<Carried>()
        // A move that landed after the week after isn't counted.
        history.all
            .filter { task ->
                val from = task.movedFromDate?.toLocalDate()
                from != null && from >= week && from <= weekEnd && task.date >= next && task.date <= nextEnd
            }
            .sortedBy { it.date }
            .forEach { task ->
                val mission = byId[task.missionId] ?: return@forEach
                if (named.add(mission.missionId)) carried += Carried.Moved(mission, task.date)
            }
        missions
            .filter { it.repeatsByInterval && it.isLiveOn(next) && it.missionId !in named }
            .filter { Points.occurrencesInWeek(it, week, history) > 0 && history.doneCount(it.missionId, week, weekEnd) == 0 }
            .forEach { carried += Carried.StillDue(it) }
        return carried.take(MAX_LINES)
    }

    /**
     * Lines for the week starting [week], at most [MAX_LINES]. A finished week lists what was carried into
     * the week after it. This week lists what is being carried so far, then longer rhythms coming due and
     * pauses ending next week; those last two aren't adjustments, so they get no wand.
     */
    fun lines(week: LocalDate, today: LocalDate, missions: List<Mission>, history: History): List<AheadLine> {
        val next = week.plus(7, DateTimeUnit.DAY)
        val nextEnd = next.plus(6, DateTimeUnit.DAY)
        val finished = week < WeekMath.weekStart(today)
        val carried = carried(week, missions, history)
        val lines = carried.mapTo(mutableListOf()) { item ->
            when (item) {
                is Carried.Moved -> line(item.mission, " moved to ${item.day.weekdayName()}.", adjusted = true)
                // Under "THE WEEK AFTER", so the words say "the week after", not "next week".
                is Carried.StillDue -> if (finished) {
                    line(item.mission, " stayed due, so it moved into the week after.", adjusted = true)
                } else {
                    // Not carried yet: it can still be done this week.
                    val overdue = Points.dueDate(item.mission, history, week) < today
                    line(item.mission, if (overdue) " is still due. If it waits, it moves into next week." else " is due this week. If it waits, it moves into next week.", adjusted = false)
                }
            }
        }
        if (!finished) {
            val named = carried.mapTo(mutableSetOf()) { it.mission.missionId }
            fun add(mission: Mission, words: String) {
                if (named.add(mission.missionId)) lines += line(mission, words, adjusted = false)
            }
            // A weekly rhythm comes due every week, so only longer ones are worth a line.
            missions
                .filter { it.repeatsByInterval && it.intervalDays() > Period.WEEK.days && it.isLiveOn(next) }
                .filter { Points.dueDate(it, history, next) in next..nextEnd }
                .forEach { add(it, " comes due next week.") }
            missions
                .filter { mission -> mission.pausedUntil?.toLocalDate()?.let { it in next..nextEnd } == true }
                .forEach { add(it, " is back next week.") }
        }
        return lines.take(MAX_LINES).ifEmpty { listOf(AheadLine(listOf(LinePart("Nothing to catch up on.")), adjusted = false)) }
    }

    private fun line(mission: Mission, words: String, adjusted: Boolean) =
        AheadLine(listOf(LinePart(mission.title, mission.pillarType), LinePart(words)), adjusted)
}
