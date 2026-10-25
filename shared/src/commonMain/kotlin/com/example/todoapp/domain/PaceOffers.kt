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
    /** A gentle start had 3 full weeks: up to its goal pace. */
    STEP_UP_GENTLE,
    /** A weekly habit had 4 full weeks and this week has room: one more a week. */
    STEP_UP,
}

/** Stored by name in PaceOffer.response. */
enum class OfferResponse { ACCEPTED, KEPT, PAUSED, NOT_YET }

data class OfferFound(val mission: Mission, val type: OfferType)

/** A piece of a sentence. With [pillar] it's a mission title, drawn as a chip in that pillar's color (spec 7.1). */
data class LinePart(val text: String, val pillar: PillarType? = null)

/** What a pace-offer sticky says. Step-ups have no [pause], and their [keep] is "Not yet". */
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
     * one per mission. [freeMinutesLeft] is this week's free time minus what its flexible missions need.
     * Weeks before a mission's last answered offer don't count, so after any answer the mission gets
     * a fresh run of weeks before it's asked again. "Not yet" also holds that offer back [SNOOZE_WEEKS].
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
        for (type in OfferType.entries) {
            for (mission in missions) {
                if (found.size >= MAX_PER_WEEK) return found
                if (found.any { it.mission.missionId == mission.missionId }) continue
                if (!mission.isLiveOn(today) || accepted(mission, type) == null) continue
                if (snoozed(mission, type, past, weekStart)) continue
                val since = past
                    .filter { it.missionId == mission.missionId && it.response != null }
                    .maxOfOrNull { MyDate(it.week).toLocalDate() }
                if (triggers(mission, type, weekStart, history, since, freeMinutesLeft)) found += OfferFound(mission, type)
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
                if (counted && weekly && goal != null && goal > mission.count) mission.copy(count = goal, goalCount = null) else null
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
        mission.copy(pausedUntil = MyDate.fromLocalDate(today.plus(1, DateTimeUnit.MONTH)))

    /** The sticky's words, or null when the offer no longer applies. Built from stored fields; the title is only shown. */
    fun words(mission: Mission, type: OfferType): OfferWords? {
        val next = accepted(mission, type) ?: return null
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
                accept = "Make it ${next.count}×",
                keep = if (mission.periodUnit == Period.DAY) "Keep every day" else "Keep ${mission.count}×",
                pause = "Pause a month",
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
                    pause = "Pause a month",
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
        since: LocalDate?,
        freeMinutesLeft: Int,
    ): Boolean {
        // The last n finished weeks, or null when some of them come before the last answer.
        fun lastWeeks(n: Int): List<LocalDate>? =
            (1..n).map { weekStart.minus(7 * it, DateTimeUnit.DAY) }.takeIf { weeks -> since == null || weeks.all { it >= since } }
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

    /** At 80% of its own points that week, the way a pillar counts as full. A week it wasn't due doesn't count. */
    private fun full(mission: Mission, week: LocalDate, history: History): Boolean {
        val due = Points.occurrencesInWeek(mission, week, history)
        if (due == 0) return false
        val end = week.plus(6, DateTimeUnit.DAY)
        val earned = history.tasksFor(mission.missionId).filter { it.date >= week && it.date <= end }.sumOf { it.pointsEarned }
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
