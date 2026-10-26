package com.example.todoapp.domain

import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** The week highlights card on the Metrics tab (spec 9), in the diary's highlights style. */
data class WeekHighlights(
    /** "THIS WEEK", "LAST WEEK" or "WEEK OF OCT 12" */
    val header: String,
    /** "54 of 62 points", with " · +4 extra" when a pillar went past its target. */
    val points: String,
    /** "3 of 4 pillars full", "All 4 pillars full so far", or "All 4 pillars full · a balanced week" once it's over. */
    val pillars: String,
    /** Shows the crown on the pillars line. */
    val balanced: Boolean,
    /** "2 balanced weeks out of the last 4" */
    val balancedWeeks: String,
)

object WeekHighlightsBuilder {

    /** The crown line looks at this many finished weeks: up to the week on screen, or before this week. */
    const val LOOK_BACK_WEEKS = 4

    /** [byWeek] holds the finished weeks by their Monday; [week] is the one on screen. */
    fun build(
        header: String,
        week: LocalDate,
        thisWeek: LocalDate,
        score: WeekScore,
        byWeek: Map<LocalDate, WeekSummary>,
        days: DayTasks,
    ): WeekHighlights {
        val pillarCount = score.pillars.size
        // Finished weeks only, since this week can still change, so this week looks at the 4 before it.
        // Away weeks aren't counted either way.
        val last = if (week < thisWeek) week else week.minus(7, DateTimeUnit.DAY)
        val counted = (0 until LOOK_BACK_WEEKS)
            .map { last.minus(7 * it, DateTimeUnit.DAY) }
            .filter { !WeekScores.isAway(it, days) }
            .mapNotNull { byWeek[it] }
        return WeekHighlights(
            header = header,
            points = points(score.pillars),
            pillars = when {
                score.away -> "The diary wasn't opened that week."
                !score.balanced -> "${score.pillars.count { it.full }} of $pillarCount pillars full"
                score.finished -> "All $pillarCount pillars full · a balanced week"
                else -> "All $pillarCount pillars full so far"
            },
            balanced = score.balanced && !score.away,
            balancedWeeks = balancedWeeks(counted.count { it.balanced }, counted.size),
        )
    }

    /** Each pillar counts up to its target; anything past it is extra, which never fills another pillar (spec 5). */
    fun points(scores: List<PillarScore>): String {
        val target = scores.sumOf { it.target }
        val earned = scores.sumOf { it.earned }
        if (target == 0) return MissionWords.pointsPhrase(earned)
        val counted = scores.sumOf { minOf(it.earned, it.target) }
        val extra = earned - counted
        return "$counted of ${MissionWords.pointsPhrase(target)}" + if (extra > 0) " · +$extra extra" else ""
    }

    private fun balancedWeeks(balanced: Int, weeks: Int): String = when {
        weeks == 0 -> "No weeks to count yet."
        weeks == 1 -> if (balanced == 1) "1 balanced week so far" else "No balanced week yet"
        else -> "$balanced balanced ${if (balanced == 1) "week" else "weeks"} out of the last $weeks"
    }
}
