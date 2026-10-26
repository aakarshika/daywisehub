package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Spec 5.2, in the order offers are picked: easing off comes first. Stored by name: add types, never rename one. */
enum class OfferType {
    /** A habit or finish line got less than half its weekly count 3 weeks in a row: one step slower. */
    LOWER,
    /** An every-so-often mission ran late 3 times in a row: one step longer between times. */
    LENGTHEN,
    /** A gentle start had 3 full weeks: one more a week, toward its goal pace. */
    STEP_UP_GENTLE,
    /** A weekly habit had 4 full weeks and this week has room: one more a week. */
    STEP_UP,
}

/**
 * Stored by name in PaceOffer.response: add answers, never rename one. [WITHDRAWN] isn't a button:
 * the mission's pace or weight was edited before the offer was answered.
 */
enum class OfferResponse { ACCEPTED, KEPT, PAUSED, NOT_YET, WITHDRAWN }

data class OfferFound(val mission: Mission, val type: OfferType)

/** A piece of a sentence. With [pillar] it's a mission title, drawn as a chip in that pillar's color (spec 7.1). */
data class LinePart(val text: String, val pillar: PillarType? = null)

/**
 * What a pace-offer sticky says. Step-ups have no [pause], and their [keep] is "Not yet". Easing off
 * has no [pause] either when a month's pause would run past a finish line's end.
 */
data class OfferWords(
    val header: String,
    val goingWell: Boolean,
    val line: List<LinePart>,
    val accept: String,
    val keep: String,
    val pause: String?,
)

/** Spec 5.2: the app offers pace changes; only an accepted offer changes a mission. */
object PaceOffers {

    const val MAX_PER_WEEK = 2
    const val SLIP_WEEKS = 3
    const val LATE_TIMES = 3
    const val GENTLE_WEEKS = 3
    const val STEADY_WEEKS = 4
    /** "Not yet" holds the same offer back this long. */
    const val SNOOZE_WEEKS = 4
    /** History read to judge offers: enough for 3 late runs of a 3-month rhythm. */
    const val HISTORY_DAYS = 400
    /** "Every day" steps down to this many a week, and a weekly habit steps up to at most this many. */
    private const val MOST_PER_WEEK = 6

    /**
     * Offers for the week starting [weekStart], judged on finished weeks only: at most [MAX_PER_WEEK],
     * one per mission. [freeMinutesLeft] is this week's free time minus what its flexible missions need;
     * two step-ups share it. A mission's run of weeks starts at its last answered offer or its last
     * pace change ([since]), so it's judged only at its current pace. "Not yet" also holds that offer
     * back [SNOOZE_WEEKS]. A finish line that ends before the end of this week gets no offer.
     */
    fun find(
        weekStart: LocalDate,
        today: LocalDate,
        missions: List<Mission>,
        history: History,
        past: List<PaceOffer>,
        freeMinutesLeft: Int,
    ): List<OfferFound> {
        val found = mutableListOf<OfferFound>()
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
        // Weeks with an item of their own. A week the app wasn't opened in breaks a run, so a holiday isn't three
        // slipped weeks. Items moved in don't count: a move can land in a week the app then never opens.
        val opened = history.all.filter { it.movedFromDate == null }.map { WeekMath.weekStart(it.date) }.toSet()
        var freeLeft = freeMinutesLeft
        for (type in OfferType.entries) {
            for (mission in missions) {
                if (found.size >= MAX_PER_WEEK) return found
                if (found.any { it.mission.missionId == mission.missionId }) continue
                if (!mission.isLiveOn(today) || accepted(mission, type) == null) continue
                if (mission.endDate?.let { it.toLocalDate() < weekEnd } == true) continue
                if (snoozed(mission, type, past, weekStart)) continue
                if (triggers(mission, type, weekStart, history, opened, since(mission, past), freeLeft)) {
                    found += OfferFound(mission, type)
                    // The next step-up has to fit in what this one leaves.
                    if (type == OfferType.STEP_UP) freeLeft -= mission.durationMinutes
                }
            }
        }
        return found
    }

    /** The mission after saying yes, or null when the offer can't apply to it (any more). */
    fun accepted(mission: Mission, type: OfferType): Mission? {
        val kind = mission.missionKind
        val counted = kind == MissionKind.HABIT || kind == MissionKind.FINISH_LINE
        val weekly = mission.periodUnit == Period.WEEK
        return when (type) {
            OfferType.LOWER -> when {
                !counted -> null
                mission.periodUnit == Period.DAY -> mission.copy(period = Period.WEEK.name, count = MOST_PER_WEEK)
                weekly && mission.count >= 2 -> mission.copy(count = mission.count - 1)
                else -> null
            }
            OfferType.LENGTHEN ->
                if (kind == MissionKind.RHYTHM && mission.intervalCount + 1 in MissionDrafts.intervalRange(mission.intervalPeriod)) {
                    mission.copy(intervalCount = mission.intervalCount + 1)
                } else null
            OfferType.STEP_UP_GENTLE -> {
                val goal = mission.goalCount
                if (counted && weekly && goal != null && goal > mission.count) {
                    // One more a week; the goal goes once it's reached, so the card drops "up to".
                    val next = minOf(goal, mission.count + 1)
                    mission.copy(count = next, goalCount = goal.takeIf { it > next })
                } else null
            }
            OfferType.STEP_UP -> {
                val gentle = (mission.goalCount ?: 0) > mission.count
                if (kind == MissionKind.HABIT && weekly && !gentle && mission.count < MOST_PER_WEEK) {
                    mission.copy(count = mission.count + 1, goalCount = null)
                } else null
            }
        }
    }

    /** "Pause a month": back on the same date next month. */
    fun paused(mission: Mission, today: LocalDate): Mission =
        mission.copy(pausedUntil = MyDate.fromLocalDate(pauseEnd(today)))

    /**
     * The answers [type]'s sticky offers [mission] on [today], matching [words]' buttons: easing off can be
     * kept or paused (no pause past a finish line's end), and a step-up can wait ("Not yet").
     */
    fun answers(mission: Mission, type: OfferType, today: LocalDate): Set<OfferResponse> = when (type) {
        OfferType.LOWER, OfferType.LENGTHEN ->
            if (canPause(mission, today)) setOf(OfferResponse.ACCEPTED, OfferResponse.KEPT, OfferResponse.PAUSED)
            else setOf(OfferResponse.ACCEPTED, OfferResponse.KEPT)
        OfferType.STEP_UP_GENTLE, OfferType.STEP_UP -> setOf(OfferResponse.ACCEPTED, OfferResponse.NOT_YET)
    }

    /**
     * True when an edit changes what the offers judge: the kind, the pace (count, period, goal, interval,
     * days) or the weight. The editor then starts the mission's run of weeks again (Mission.paceSince).
     */
    fun paceChanged(before: Mission, after: Mission): Boolean =
        before.kind != after.kind || before.count != after.count || before.period != after.period ||
            before.goalCount != after.goalCount || before.intervalCount != after.intervalCount ||
            before.intervalUnit != after.intervalUnit || before.weekDays != after.weekDays || before.weight != after.weight

    /** The sticky's words on [today], or null when the offer no longer applies. Built from stored fields; the title is only shown. */
    fun words(mission: Mission, type: OfferType, today: LocalDate): OfferWords? {
        val next = accepted(mission, type) ?: return null
        val pause = "Pause a month".takeIf { canPause(mission, today) }
        val title = LinePart(mission.title, mission.pillarType)
        return when (type) {
            OfferType.LOWER -> OfferWords(
                header = "A SMALL CHANGE?",
                goingWell = false,
                line = listOf(
                    title,
                    LinePart(
                        " has slipped $SLIP_WEEKS weeks in a row. That's fine. Want to make it ${weeklyPace(next.count)} " +
                            "for now? You can step back up anytime."
                    ),
                ),
                accept = if (next.count == 1) "Make it once a week" else "Make it ${next.count}×",
                keep = if (mission.periodUnit == Period.DAY) "Keep every day" else "Keep ${mission.count}×",
                pause = pause,
            )
            OfferType.LENGTHEN -> {
                val longer = MissionWords.intervalPhrase(next.intervalCount, next.intervalPeriod)
                OfferWords(
                    header = "A SMALL CHANGE?",
                    goingWell = false,
                    line = listOf(
                        title,
                        LinePart(" has slipped $LATE_TIMES times in a row. That's fine. Want to make it $longer for now? You can step back up anytime."),
                    ),
                    accept = longer.replaceFirstChar { it.uppercase() },
                    keep = "Keep ${MissionWords.intervalPhrase(mission.intervalCount, mission.intervalPeriod)}",
                    pause = pause,
                )
            }
            OfferType.STEP_UP_GENTLE -> OfferWords(
                header = "GOING WELL",
                goingWell = true,
                line = listOf(title, LinePart(" has gone well $GENTLE_WEEKS weeks running. Ready for ${weeklyPace(next.count)}?")),
                accept = "Step up",
                keep = "Not yet",
                pause = null,
            )
            OfferType.STEP_UP -> OfferWords(
                header = "GOING WELL",
                goingWell = true,
                line = listOf(title, LinePart(" has gone well $STEADY_WEEKS weeks running. Ready for ${weeklyPace(next.count)}?")),
                accept = "Step up",
                keep = "Not yet",
                pause = null,
            )
        }
    }

    private fun weeklyPace(count: Int): String = if (count == 1) "once a week" else "$count× a week"

    private fun pauseEnd(today: LocalDate): LocalDate = today.plus(1, DateTimeUnit.MONTH)

    /** A month's pause mustn't run past a finish line's end. */
    private fun canPause(mission: Mission, today: LocalDate): Boolean =
        mission.endDate?.let { pauseEnd(today) <= it.toLocalDate() } ?: true

    /**
     * Where [mission]'s run of weeks starts: its last answered offer's week, or the day its pace last
     * changed, whichever is later. Weeks before its start date weren't due, so they break a run anyway.
     */
    private fun since(mission: Mission, past: List<PaceOffer>): LocalDate? {
        val answered = past
            .filter { it.missionId == mission.missionId && it.response != null }
            .maxOfOrNull { MyDate(it.week).toLocalDate() }
        val paced = mission.paceSince?.let { MyDate(it).toLocalDate() }
        return listOfNotNull(answered, paced).maxOrNull()
    }

    private fun snoozed(mission: Mission, type: OfferType, past: List<PaceOffer>, weekStart: LocalDate): Boolean =
        past.any { offer ->
            offer.missionId == mission.missionId && offer.type == type.name &&
                offer.snoozeUntil?.let { weekStart < MyDate(it).toLocalDate() } == true
        }

    private fun triggers(
        mission: Mission,
        type: OfferType,
        weekStart: LocalDate,
        history: History,
        opened: Set<LocalDate>,
        since: LocalDate?,
        freeMinutesLeft: Int,
    ): Boolean {
        // The last n finished weeks, or null when one starts before [since] or wasn't opened.
        fun lastWeeks(n: Int): List<LocalDate>? =
            (1..n).map { weekStart.minus(7 * it, DateTimeUnit.DAY) }
                .takeIf { weeks -> weeks.all { (since == null || it >= since) && it in opened } }
        return when (type) {
            OfferType.LOWER -> lastWeeks(SLIP_WEEKS)?.all { slipped(mission, it, history) } ?: false
            OfferType.LENGTHEN -> ranLate(mission, weekStart, history, since)
            OfferType.STEP_UP_GENTLE -> lastWeeks(GENTLE_WEEKS)?.all { full(mission, it, history) } ?: false
            OfferType.STEP_UP -> freeMinutesLeft >= mission.durationMinutes &&
                (lastWeeks(STEADY_WEEKS)?.all { full(mission, it, history) } ?: false)
        }
    }

    /** Done less than half as often as it was due that week. A week it wasn't due doesn't count. */
    private fun slipped(mission: Mission, week: LocalDate, history: History): Boolean {
        val due = Points.occurrencesInWeek(mission, week, history)
        val done = history.doneCount(mission.missionId, week, week.plus(6, DateTimeUnit.DAY))
        return due > 0 && done * 2 < due
    }

    /**
     * At 80% of its own points that week, the way a pillar counts as full. Points are worked out from what
     * was done, at today's weight, so a week stored at an older weight is judged like the rest.
     * A week it wasn't due doesn't count.
     */
    private fun full(mission: Mission, week: LocalDate, history: History): Boolean {
        val due = Points.occurrencesInWeek(mission, week, history)
        if (due == 0) return false
        val end = week.plus(6, DateTimeUnit.DAY)
        val earned = history.tasksFor(mission.missionId)
            .filter { it.date >= week && it.date <= end }
            .sumOf { Points.forTask(mission, it.taskState) }
        return Points.isFull(earned, mission.weight * due)
    }

    /**
     * The last [LATE_TIMES] times it came due (since [since]) were all done in a later week than the
     * due date's, counting one still not done when its week has already finished.
     */
    private fun ranLate(mission: Mission, weekStart: LocalDate, history: History, since: LocalDate?): Boolean {
        val interval = mission.intervalDays()
        val doneDays = history.tasksFor(mission.missionId)
            .filter { it.taskState.countsAsDone && it.date < weekStart }
            .map { it.date }.distinct().sorted()
        val start = mission.startDate.toLocalDate()
        // When the start is older than the history read, the first done day only sets the next due date.
        var due: LocalDate? = if (start >= weekStart.minus(HISTORY_DAYS, DateTimeUnit.DAY)) start else null
        val runs = mutableListOf<Pair<LocalDate, Boolean>>()
        for (day in doneDays) {
            due?.let { runs += it to (WeekMath.weekStart(day) > WeekMath.weekStart(it)) }
            due = day.plus(interval, DateTimeUnit.DAY)
        }
        due?.let { if (WeekMath.weekStart(it) < weekStart) runs += it to true }
        val recent = runs.filter { since == null || it.first >= since }.takeLast(LATE_TIMES)
        return recent.size == LATE_TIMES && recent.all { it.second }
    }
}
