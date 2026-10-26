package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * "What next week adjusts" on the Metrics tab (spec 9), from stored data only. Each line names its
 * mission on a chip (spec 7.1), and the words around the chip never depend on the title. Nothing here
 * guesses why a week went the way it did: there's no "Love was light, so…".
 */
object WeekAhead {

    const val MAX_LINES = 4

    /**
     * Lines for the week starting [week]. A finished week lists what was carried into the week after
     * it. This week also lists longer rhythms coming due and pauses ending next week. [missions] are the
     * active ones, so an archived mission never shows here.
     */
    fun lines(week: LocalDate, thisWeek: LocalDate, missions: List<Mission>, days: DayTasks, history: History): List<List<LinePart>> {
        val weekEnd = week.plus(6, DateTimeUnit.DAY)
        val next = week.plus(7, DateTimeUnit.DAY)
        val nextEnd = next.plus(6, DateTimeUnit.DAY)
        val finished = week < thisWeek
        val byId = missions.associateBy { it.missionId }
        val named = mutableSetOf<Long>()
        val lines = mutableListOf<List<LinePart>>()
        fun add(mission: Mission, words: String) {
            if (named.add(mission.missionId)) lines += listOf(LinePart(mission.title, mission.pillarType), LinePart(words))
        }
        // "Move it" items already carried from this week into the next (spec 6.4).
        days.between(next, nextEnd)
            .filter { task -> task.movedFromDate?.toLocalDate()?.let { it >= week && it <= weekEnd } == true }
            .forEach { task -> byId[task.missionId]?.let { add(it, " moved to ${task.date.weekdayName()}.") } }
        // Every-so-often missions stay due until they're done (spec 5.1). Shorter rhythms are due every week anyway.
        val rhythms = missions.filter { it.repeatsByInterval && it.intervalDays() >= Period.WEEK.days && it.isLiveOn(next) }
        rhythms
            .filter { Points.occurrencesInWeek(it, week, history) > 0 && history.doneCount(it.missionId, week, weekEnd) == 0 }
            .forEach {
                add(it, if (finished) " stayed due, so it moved into the next week." else " is still due. If it waits, it moves into next week.")
            }
        if (!finished) {
            // A weekly rhythm comes due every week, so only longer ones are worth a line.
            rhythms
                .filter { it.intervalDays() > Period.WEEK.days && Points.dueDate(it, history, next) in next..nextEnd }
                .forEach { add(it, " comes due next week.") }
            missions
                .filter { mission -> mission.pausedUntil?.toLocalDate()?.let { it in next..nextEnd } == true }
                .forEach { add(it, " is back next week.") }
        }
        return lines.take(MAX_LINES).ifEmpty { listOf(listOf(LinePart("Nothing to catch up on."))) }
    }
}
