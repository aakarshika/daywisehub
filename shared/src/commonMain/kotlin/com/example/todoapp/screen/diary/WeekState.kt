package com.example.todoapp.screen.diary

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.History
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.OfferType
import com.example.todoapp.domain.OfferWords
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.date
import com.example.todoapp.domain.earned
import com.example.todoapp.domain.isLiveOn
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.pillarsFull
import com.example.todoapp.domain.repeatsByInterval
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.target
import com.example.todoapp.domain.weekStartDate
import com.example.todoapp.domain.weekdayName
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Monday's "Last week" card (spec 9), from last week's stored summary. */
data class LastWeekState(
    /** "Oct 26 to Nov 1" */
    val range: String,
    /** "54 of 62 points", with " · +4 extra" when a pillar went past its target. */
    val points: String,
    /** "3 of 4 pillars full", or "All 4 pillars full · a balanced week". */
    val pillars: String,
    val balanced: Boolean,
    /** What the app adjusted, each line naming its mission on a chip. Never empty. */
    val adjusted: List<List<LinePart>>,
)

/** One pace-offer sticky (spec 5.2). */
data class OfferNote(val offerId: Long, val words: OfferWords)

/** What the diary reads besides missions and tasks. */
data class DiaryExtras(
    /** Plans from the start of this day's week to a week after today, for "opens the week" and room ahead. */
    val plans: List<DayPlan>,
    /** Free minutes per weekday, for days ahead that have no plan yet. */
    val settings: List<DaySettings>,
    /** The summary of the week before this day's week. */
    val lastWeek: WeekSummary?,
    /** Offers made for the week that has today in it. */
    val offers: List<PaceOffer>,
)

object WeekStateBuilder {

    const val MAX_ADJUSTED = 3

    /**
     * True when [date] opens its week in the diary: no earlier day of that week has a plan, because the
     * diary wasn't opened on it. Monday always opens its week. The "Last week" card shows on such days.
     */
    fun opensWeek(date: LocalDate, plans: List<DayPlan>): Boolean {
        val weekStart = WeekMath.weekStart(date)
        return plans.none { MyDate(it.date).toLocalDate().let { day -> day >= weekStart && day < date } }
    }

    /** Null without a summary, or when last week had nothing due and nothing done. */
    fun lastWeek(summary: WeekSummary?, missions: List<Mission>, tasks: List<TodayTask>): LastWeekState? {
        summary ?: return null
        val pillars = PillarType.scoredPillars
        val target = pillars.sumOf { summary.target(it) }
        val earned = pillars.sumOf { summary.earned(it) }
        if (target == 0 && earned == 0) return null
        // Extra never fills another pillar (spec 5), so it's shown on its own.
        val counted = pillars.sumOf { minOf(summary.earned(it), summary.target(it)) }
        val extra = earned - counted
        val start = summary.weekStartDate
        return LastWeekState(
            range = "${start.shortLabel()} to ${start.plus(6, DateTimeUnit.DAY).shortLabel()}",
            points = "$counted of ${MissionWords.pointsPhrase(target)}" + if (extra > 0) " · +$extra extra" else "",
            pillars = if (summary.balanced) {
                "All ${pillars.size} pillars full · a balanced week"
            } else {
                "${summary.pillarsFull()} of ${pillars.size} pillars full"
            },
            balanced = summary.balanced,
            adjusted = adjusted(start, missions, tasks),
        )
    }

    /** This week's unanswered offers whose mission is still active and can still take the change. */
    fun offers(offers: List<PaceOffer>, missions: List<Mission>, today: LocalDate): List<OfferNote> {
        val byId = missions.associateBy { it.missionId }
        return offers.filter { it.response == null }.mapNotNull { offer ->
            val mission = byId[offer.missionId] ?: return@mapNotNull null
            val type = OfferType.entries.firstOrNull { it.name == offer.type } ?: return@mapNotNull null
            PaceOffers.words(mission, type, today)?.let { OfferNote(offer.offerId, it) }
        }
    }

    /**
     * What the app adjusted, from stored data only: items "move it" carried from last week into this
     * week, and every-so-often missions that were due last week and stay due (spec 5.1).
     */
    private fun adjusted(lastStart: LocalDate, missions: List<Mission>, tasks: List<TodayTask>): List<List<LinePart>> {
        val lastEnd = lastStart.plus(6, DateTimeUnit.DAY)
        val thisStart = lastStart.plus(7, DateTimeUnit.DAY)
        val byId = missions.associateBy { it.missionId }
        val history = History(tasks)
        val named = mutableSetOf<Long>()
        val lines = mutableListOf<List<LinePart>>()
        tasks
            .filter { task ->
                val from = task.movedFromDate?.toLocalDate()
                from != null && from >= lastStart && from <= lastEnd && task.date >= thisStart
            }
            .sortedBy { it.date }
            .forEach { task ->
                val mission = byId[task.missionId] ?: return@forEach
                if (named.add(mission.missionId)) lines += listOf(chip(mission), LinePart(" moved to ${task.date.weekdayName()}."))
            }
        missions
            .filter { it.repeatsByInterval && it.isLiveOn(thisStart) && it.missionId !in named }
            .filter {
                Points.occurrencesInWeek(it, lastStart, history) > 0 && history.doneCount(it.missionId, lastStart, lastEnd) == 0
            }
            .forEach { lines += listOf(chip(it), LinePart(" is still due, so it moved into this week.")) }
        return lines.take(MAX_ADJUSTED).ifEmpty { listOf(listOf(LinePart("Nothing to catch up on."))) }
    }

    private fun chip(mission: Mission) = LinePart(mission.title, mission.pillarType)
}
