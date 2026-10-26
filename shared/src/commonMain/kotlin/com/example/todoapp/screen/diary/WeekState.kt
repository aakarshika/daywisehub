package com.example.todoapp.screen.diary

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.AheadLine
import com.example.todoapp.domain.Carried
import com.example.todoapp.domain.History
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.OfferType
import com.example.todoapp.domain.OfferWords
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.WeekAhead
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.chip
import com.example.todoapp.domain.earned
import com.example.todoapp.domain.pillarsFull
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
    /**
     * "54 of 62 points", with " · +4 extra" when a pillar went past its target; just "+4 extra" when no pillar
     * had a target, and "No points to count." when the diary wasn't opened that week.
     */
    val points: String,
    /**
     * "3 of 4 pillars full", "All 4 pillars full · a balanced week", "Nothing was due that week.", or
     * "The diary wasn't opened that week.", as on the Metrics tab.
     */
    val pillars: String,
    /** Shows the crown on the pillars line; never on a week the diary wasn't opened in. */
    val balanced: Boolean,
    /**
     * What the app adjusted, each line naming its mission on a chip. Never empty on today's page. Empty on
     * an earlier day's, which can't say what was adjusted: that reads today's missions. Only an adjusted
     * line gets the wand; "Nothing to catch up on." doesn't.
     */
    val adjusted: List<AheadLine>,
)

/** One pace-offer sticky (spec 5.2). */
data class OfferNote(val offerId: Long, val words: OfferWords)

/** What the diary reads besides missions and tasks. */
data class DiaryExtras(
    /** Plans from the start of this day's week to a week after today (after this day, on a page older than yesterday), for "opens the week" and room ahead. */
    val plans: List<DayPlan>,
    /** Free minutes per weekday, for days ahead that have no plan yet. */
    val settings: List<DaySettings>,
    /** The summary of the week before this day's week. */
    val lastWeek: WeekSummary?,
    /** The summary of this day's week, once that week is over; null for this week and later ones. */
    val pageWeek: WeekSummary?,
    /** Offers made for the week that has today in it; empty on any page but today's. */
    val offers: List<PaceOffer>,
)

object WeekStateBuilder {

    /**
     * True when [date] opens its week in the diary: Monday, or the first day of its week that has a plan
     * (the diary was opened on it) when no earlier day of that week has one. A day that was never opened
     * and isn't Monday doesn't open the week, so the "Last week" card isn't repeated there. [plans] start
     * at the week's Monday. The card shows on such days.
     */
    fun opensWeek(date: LocalDate, plans: List<DayPlan>): Boolean {
        val key = MyDate.fromLocalDate(date).dateString
        if (plans.any { it.date < key }) return false
        return date == WeekMath.weekStart(date) || plans.any { it.date == key }
    }

    /**
     * Null without a summary, or when last week had nothing due and nothing done. [away] is true when the
     * diary wasn't opened that week (WeekScores.isAway): its zeros aren't misses, so the card shows no
     * numbers and no crown, as the Metrics tab does; the adjusted lines stay. [withAdjusted] is false on
     * an earlier day's page: what the app adjusted reads today's missions, so that page leaves it out.
     */
    fun lastWeek(summary: WeekSummary?, away: Boolean, missions: List<Mission>, history: History, withAdjusted: Boolean): LastWeekState? {
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
            points = when {
                away -> "No points to count."
                target == 0 -> "+$extra extra"
                else -> "$counted of ${MissionWords.pointsPhrase(target)}" + if (extra > 0) " · +$extra extra" else ""
            },
            // The same lines as the Metrics tab's highlights card (WeekHighlightsBuilder).
            pillars = when {
                away -> "The diary wasn't opened that week."
                target == 0 -> "Nothing was due that week."
                summary.balanced -> "All ${pillars.size} pillars full · a balanced week"
                else -> "${summary.pillarsFull()} of ${pillars.size} pillars full"
            },
            balanced = summary.balanced && !away,
            adjusted = if (withAdjusted) adjusted(start, missions, history) else emptyList(),
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
     * week, and every-so-often missions that were due last week and stay due (spec 5.1). The rule is
     * WeekAhead.carried, which the Metrics tab reads too, so the two never disagree about a week.
     */
    private fun adjusted(lastStart: LocalDate, missions: List<Mission>, history: History): List<AheadLine> =
        WeekAhead.carried(lastStart, missions, history)
            .map { item ->
                val words = when (item) {
                    is Carried.Moved -> " moved to ${item.day.weekdayName()}."
                    is Carried.StillDue -> " is still due, so it moved into this week."
                }
                AheadLine(listOf(chip(item.mission), LinePart(words)), adjusted = true)
            }
            .ifEmpty { listOf(AheadLine(listOf(LinePart("Nothing to catch up on.")), adjusted = false)) }
}
